package org.openjwc.client.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.source.SourceRegistry
import org.openjwc.client.data.source.SourceRunner
import org.openjwc.client.log.Logger
import org.openjwc.client.notification.NewsNotifier
import org.openjwc.client.script.QuickJsScriptHost
import java.util.concurrent.TimeUnit

/**
 * 后台抓取已订阅的本地数据源。
 * 首次抓取静默建立通知水位；此后仅对新增条目发通知（通知开关关闭时只更新缓存）。
 */
class SourceCrawlWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        return try {
            val settings = SettingsDataSource(context).userSettings.first()
            val database = AppDatabase.getDatabase(context)
            val registry = SourceRegistry(context, database.sourceDao(), SettingsDataSource(context))
            registry.syncBuiltIns()

            val subscribed = database.sourceDao().getSubscribed()
            if (subscribed.isEmpty()) {
                Logger.d(TAG, "没有已订阅数据源，跳过")
                return Result.success()
            }
            Logger.i(TAG, "开始后台抓取：${subscribed.size} 个数据源，回溯 ${settings.crawlDaysGap} 天")

            val runner = SourceRunner(
                registry,
                database.sourceDao(),
                database.noticeDao(),
                SourceCrawlScheduler.newScriptHost(),
            )

            for (source in subscribed) {
                val outcome = runner.crawl(source, settings.crawlDaysGap)
                val toNotify = runner.settleNotifications(
                    outcome,
                    notify = settings.newsNotificationEnabled,
                )
                Logger.d(
                    TAG,
                    "数据源 ${source.id} 抓取 ${outcome.notices.size} 条（新增 ${outcome.newNotices.size}）" +
                        outcome.error?.let { " 失败：$it" }.orEmpty(),
                )
                if (toNotify.isNotEmpty()) {
                    Logger.i(TAG, "数据源 ${source.id} 新资讯 ${toNotify.size} 条，发送通知")
                    NewsNotifier.postNewNews(context, toNotify)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Logger.e(TAG, "后台抓取失败: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "source_crawl"
        private const val ONCE_WORK_NAME = "source_crawl_once"
        private const val TAG = "SourceCrawlWorker"
    }
}

/** 抓取任务的排程与共享脚本宿主。 */
object SourceCrawlScheduler {
    private const val MIN_INTERVAL_MINUTES = 15

    /** 脚本抓取用的 OkHttpClient：不挂日志拦截器，避免把正文写进 logcat。 */
    fun newScriptHost(): QuickJsScriptHost = QuickJsScriptHost(
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    )

    /**
     * 同步周期抓取任务：有已订阅数据源则排程，否则取消。
     * 周期取「开启通知时用通知间隔，否则用数据源声明的最小 `@schedule`」，下限 15 分钟。
     * 使用 UPDATE 策略，重复调用不会重置已有周期。
     */
    suspend fun sync(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val subscribed = AppDatabase.getDatabase(context).sourceDao().getSubscribed()
        if (subscribed.isEmpty()) {
            workManager.cancelUniqueWork(SourceCrawlWorker.WORK_NAME)
            return
        }

        val settings = SettingsDataSource(context).userSettings.first()
        val interval = (
            if (settings.newsNotificationEnabled) {
                settings.newsCheckIntervalMinutes
            } else {
                subscribed.minOf { it.scheduleMinutes }
            }
            ).coerceAtLeast(MIN_INTERVAL_MINUTES)
            .toLong()

        val request = PeriodicWorkRequestBuilder<SourceCrawlWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            SourceCrawlWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** 立即后台抓取一次（不阻塞调用方）。 */
    fun runOnce(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "source_crawl_once",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SourceCrawlWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build(),
        )
    }
}
