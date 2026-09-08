package org.openjwc.client.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import org.openjwc.client.data.datastore.SettingsDataSource
import java.util.concurrent.TimeUnit

object NewsNotificationScheduler {
    const val MIN_INTERVAL_MINUTES = 15

    /**
     * 按当前设置同步周期任务：开启则以 [intervalMinutes] 周期检查新资讯，关闭则取消。
     * 使用 UPDATE 策略，重复调用不会重置已有的执行周期。
     */
    suspend fun sync(context: Context) {
        val settings = SettingsDataSource(context).userSettings.first()
        val workManager = WorkManager.getInstance(context)

        if (!settings.newsNotificationEnabled) {
            workManager.cancelUniqueWork(NewsCheckWorker.WORK_NAME)
            return
        }

        val interval = settings.newsCheckIntervalMinutes
            .coerceAtLeast(MIN_INTERVAL_MINUTES)
            .toLong()

        val request = PeriodicWorkRequestBuilder<NewsCheckWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            NewsCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * 立即执行一次检查：用于开启通知开关时给出即时反馈，
     * 结果与周期任务一致（基线静默 / 新资讯发通知）。
     */
    fun runOnce(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONCE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<NewsCheckWorker>().build()
        )
    }

    private const val ONCE_WORK_NAME = "news_check_once"
}
