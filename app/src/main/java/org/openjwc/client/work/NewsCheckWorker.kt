package org.openjwc.client.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.repository.NewsRepository
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.notification.NewsNotifier

/**
 * 定期检查服务器上的新资讯并发通知。
 * 资讯写入 Room 缓存表，`notified` 字段即通知水位：
 * 分区（host+port+label）为空时静默建立基线；清除缓存后同样静默重建。
 */
class NewsCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val tag = "NewsCheckWorker"

    override suspend fun doWork(): Result {
        return try {
            checkAndNotify()
            Result.success()
        } catch (e: Exception) {
            Logger.e(tag, "Check failed: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun checkAndNotify() {
        val settingsDataSource = SettingsDataSource(applicationContext)
        val settings = settingsDataSource.userSettings.first()
        if (!settings.newsNotificationEnabled) return

        val authDataSource = AuthDataSource(applicationContext)
        val session = authDataSource.authSession.first()
        val token = session.token ?: return
        if (!session.isLoggedIn) return

        val repository = NewsRepository(
            AppDatabase.getDatabase(applicationContext).newsDao(),
            settingsDataSource,
            authDataSource
        )

        val labelResult = repository.getLabels()
        val labels = (labelResult as? NetworkResult.Success)?.response?.data?.labels ?: return
        runCatching { repository.saveLabelsCache(labels) }

        for (label in labels) {
            val result = repository.getNews(label, page = 1, size = PAGE_SIZE)
            val fetched = (result as? NetworkResult.Success)?.response?.data?.fetchedNotices
                .takeUnless { it.isNullOrEmpty() } ?: continue

            try {
                val oldCount = repository.countCachedNews(label)
                val newlyInserted = repository.upsertNewsCachePreservingNotified(fetched)
                runCatching { repository.pruneNewsCache(label) }

                when {
                    oldCount == 0 -> {
                        // 首跑/清缓存后：只建立水位，不把历史资讯全推给用户
                        repository.markNewsNotified(fetched.map { it.id })
                    }

                    newlyInserted.isNotEmpty() -> {
                        NewsNotifier.postNewNews(applicationContext, newlyInserted)
                        repository.markNewsNotified(newlyInserted.map { it.id })
                    }
                }
            } catch (e: Exception) {
                // 单个标签失败不中断整轮
                Logger.e(tag, "Label '$label' failed: ${e.message}", e)
            }
        }
    }

    companion object {
        const val WORK_NAME = "news_check"
        private const val PAGE_SIZE = 20
    }
}
