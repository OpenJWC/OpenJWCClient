package org.openjwc.client.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.openjwc.client.agent.AgentLoop
import org.openjwc.client.agent.AgentLoopFactory
import org.openjwc.client.agent.AgentRequest
import org.openjwc.client.agent.PromptTemplates
import org.openjwc.client.data.dao.DailyReportDao
import org.openjwc.client.data.dao.NoticeDao
import org.openjwc.client.data.models.DailyReportEntity
import org.openjwc.client.data.models.DailyReportStatus
import org.openjwc.client.log.Logger
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 本地日报：按日取语料 → 每批 [BATCH_SIZE] 条交给 Agent 摘要 → 多批时再合并 → 落库。
 * 状态机与后端一致：`running → failed | completed`，只有 `completed` 对外可见。
 */
class DailyReportRepository(
    private val dailyReportDao: DailyReportDao,
    private val noticeDao: NoticeDao,
    private val agentLoopFactory: AgentLoopFactory,
) : DailyReportSource {
    private val tag = "DailyReportRepository"
    private val mutex = Mutex()

    /** 已有日报的日期（倒序）。 */
    fun observeCompletedDays(): Flow<List<String>> =
        dailyReportDao.observeAll().map { reports ->
            reports.filter { it.status == DailyReportStatus.COMPLETED.value }.map { it.day }
        }

    suspend fun latestCompleted(before: String): DailyReportEntity? =
        dailyReportDao.latestCompleted(before)

    suspend fun getCompleted(day: String): DailyReportEntity? =
        dailyReportDao.get(day)?.takeIf { it.status == DailyReportStatus.COMPLETED.value }

    /** Agent 工具用：只暴露已完成日报的正文。 */
    override suspend fun completedReport(day: String): String? = getCompleted(day)?.content

    /** 任意状态的日报记录（用于展示失败原因）。 */
    suspend fun getRecord(day: String): DailyReportEntity? = dailyReportDao.get(day)

    /** 是否已有某日的日报（任意状态）。 */
    suspend fun statusOf(day: String): DailyReportStatus? =
        dailyReportDao.get(day)?.let { DailyReportStatus.from(it.status) }

    /**
     * 生成指定日期的日报。已完成时直接返回；失败不会发布半成品。
     * @return 成功或失败原因
     */
    suspend fun generate(day: String): Result<Unit> = mutex.withLock {
        runCatching { LocalDate.parse(day, DATE_FORMAT) }.getOrElse {
            return Result.failure(IllegalArgumentException("日报日期必须为 YYYY-MM-DD"))
        }

        if (statusOf(day) == DailyReportStatus.COMPLETED) return Result.success(Unit)

        val ids = noticeDao.idsByDay(day)
        if (ids.size > MAX_SOURCES) {
            return Result.failure(IllegalStateException("当日资讯超过 $MAX_SOURCES 条，暂不生成日报"))
        }
        val now = System.currentTimeMillis()
        dailyReportDao.save(day, DailyReportStatus.RUNNING.value, "", ids.size, null, now)

        if (ids.isEmpty()) {
            dailyReportDao.save(
                day, DailyReportStatus.COMPLETED.value, "当日没有已收录的资讯。", 0, null,
                System.currentTimeMillis()
            )
            return Result.success(Unit)
        }

        return try {
            val loop = agentLoopFactory.create()
            val parts = ids.chunked(BATCH_SIZE).map { batch ->
                loop.answer(
                    AgentRequest(query = PromptTemplates.dailyBatchQuery(day), noticeIds = batch)
                )
            }
            val content = if (parts.size <= 1) {
                parts.firstOrNull().orEmpty()
            } else {
                mergeParts(loop, day, parts)
            }
            dailyReportDao.save(
                day, DailyReportStatus.COMPLETED.value, content, ids.size, null,
                System.currentTimeMillis()
            )
            Logger.d(tag, "日报 $day 生成完成：${ids.size} 条，${content.length} 字")
            Result.success(Unit)
        } catch (e: CancellationException) {
            dailyReportDao.save(
                day, DailyReportStatus.FAILED.value, "", ids.size, "已取消", System.currentTimeMillis()
            )
            throw e
        } catch (e: Exception) {
            Logger.e(tag, "日报 $day 生成失败: ${e.message}", e)
            // 把失败原因落库，页面才能告诉用户为什么没有日报
            dailyReportDao.save(
                day, DailyReportStatus.FAILED.value, "", ids.size,
                (e.localizedMessage ?: e.message ?: "未知错误").take(300),
                System.currentTimeMillis()
            )
            Result.failure(e)
        }
    }

    /** 多批摘要合并；拼接后超出上下文预算时退回直接拼接，避免整篇失败。 */
    private suspend fun mergeParts(loop: AgentLoop, day: String, parts: List<String>): String {
        val joined = parts.joinToString("\n\n")
        if (joined.toByteArray(Charsets.UTF_8).size > MAX_MERGE_BYTES) {
            Logger.w(tag, "日报 $day 摘要过长，跳过合并轮")
            return joined
        }
        return loop.answer(AgentRequest(query = PromptTemplates.dailyMergeQuery(day, joined)))
    }

    companion object {
        /** 每批交给 Agent 的资讯条数（对齐后端）。 */
        const val BATCH_SIZE = 8

        /** 单日资讯条数上限（对齐后端）。 */
        const val MAX_SOURCES = 100

        private const val MAX_MERGE_BYTES = 48 * 1024

        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
