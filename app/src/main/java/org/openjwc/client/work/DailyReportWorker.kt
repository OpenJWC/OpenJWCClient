package org.openjwc.client.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.openjwc.client.agent.AgentFailure
import org.openjwc.client.agent.AgentLoopFactory
import org.openjwc.client.agent.AgentRunFailedException
import org.openjwc.client.data.datastore.LlmKeyStore
import org.openjwc.client.data.datastore.LlmSettingsDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.repository.CourseRepository
import org.openjwc.client.data.repository.DailyReportRepository
import org.openjwc.client.data.repository.NewsRepository
import org.openjwc.client.log.Logger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * 定时生成前一天的日报。未开启日报或未配置模型时直接成功返回。
 */
class DailyReportWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settingsDataSource = SettingsDataSource(context)
        val settings = settingsDataSource.userSettings.first()
        if (!settings.dailyReportEnabled) return Result.success()

        return try {
            val database = AppDatabase.getDatabase(context)
            val corpus = NewsRepository(database.noticeDao(), database.sourceDao())
            val repository = DailyReportRepository(
                database.dailyReportDao(),
                database.noticeDao(),
                AgentLoopFactory(
                    LlmSettingsDataSource(context),
                    LlmKeyStore(context),
                    corpus,
                    CourseRepository(database.courseDao(), database.tableDao()),
                ),
            )
            val day = LocalDate.now().minusDays(1).toString()
            val result = repository.generate(day)
            if (result.isSuccess) return Result.success()

            val error = result.exceptionOrNull()
            Logger.e(TAG, "日报 $day 生成失败: ${error?.message}")
            // 没填 Key / Key 无效这类配置问题，重试也没用，直接失败（原因已落库，页面可见）
            val code = (error as? AgentRunFailedException)?.code
            if (code in AgentFailure.CONFIG_RELATED) Result.failure() else Result.retry()
        } catch (e: Exception) {
            Logger.e(TAG, "日报任务异常: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "daily_report"
        private const val TAG = "DailyReportWorker"
    }
}

/** 日报定时任务的排程。 */
object DailyReportScheduler {
    /** 按设置同步：开启则每天在 [UserSettings.dailyReportTime] 生成前一天日报。 */
    suspend fun sync(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val settings = SettingsDataSource(context).userSettings.first()

        if (!settings.dailyReportEnabled) {
            workManager.cancelUniqueWork(DailyReportWorker.WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<DailyReportWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(millisUntilNext(settings.dailyReportTime), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            DailyReportWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** 距离下一个 `HH:mm` 的毫秒数；解析失败时退化为 1 小时后。 */
    internal fun millisUntilNext(time: String, now: LocalDateTime = LocalDateTime.now()): Long {
        val target = runCatching { LocalTime.parse(time, TIME_FORMAT) }.getOrNull()
            ?: return TimeUnit.HOURS.toMillis(1)
        var next = now.withHour(target.hour).withMinute(target.minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
            now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return millis.coerceAtLeast(0)
    }

    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
}
