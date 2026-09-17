package org.openjwc.client.data.source

import org.openjwc.client.data.dao.NoticeDao
import org.openjwc.client.data.dao.SourceDao
import org.openjwc.client.data.models.NOTICE_CONTENT_VERSION
import org.openjwc.client.data.models.SourceEntity
import org.openjwc.client.data.models.toNoticeEntity
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.script.QuickJsScriptHost
import org.openjwc.client.script.ScriptException
import org.openjwc.client.script.ScriptNotice
import org.openjwc.client.script.ScriptOutcome
import org.openjwc.client.script.ScriptRunParams
import org.openjwc.client.script.ScriptSandbox

private const val DEFAULT_TIMEOUT_MS = 240_000L

/** 单次脚本允许的 HTTP 次数（翻页 + 正文，含首轮回溯）。 */
private const val MAX_HTTP_CALLS = 600

/** 单次脚本允许下载的字节上限（120 条正文 + 翻页，按字符数计）。 */
private const val MAX_BYTES = 32L * 1024 * 1024

/** 运行结果（含警告样本）的保存上限。 */
private const val MAX_WARNING_CHARS = 2000

/** 单次抓取的结果。 */
data class CrawlOutcome(
    val sourceId: String,
    val sourceName: String,
    val notices: List<FetchedNotice>,
    /** 相对抓取前新增的条目。 */
    val newNotices: List<FetchedNotice>,
    /** 抓取前该数据源没有条目（首次抓取）：用于静默建立通知水位。 */
    val baseline: Boolean,
    /** 致命错误：本次结果未入库。 */
    val error: String?,
    /** 非致命警告（部分条目失败、栏目结构可疑等），已入库但需要提示。 */
    val warning: String? = null,
    val scanned: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    /** 已入库但正文没抓到的条目数。 */
    val noContent: Int = 0,
    /** 其中因「仅校内 IP 可访问」而暂缺的条数。 */
    val restricted: Int = 0,
    val skippedOld: Int = 0,
    val skippedDuplicate: Int = 0,
    val skippedKnown: Int = 0,
) {
    val success: Boolean get() = error == null
}

/**
 * 执行数据源脚本：抓取 → 归一化 → 写入资讯语料 → 回写运行结果。
 * 语料按 id 去重且不裁剪；重复抓取会刷新正文，但保留 `favorite` / `notified`。
 */
class SourceRunner(
    private val registry: SourceRegistry,
    private val sourceDao: SourceDao,
    private val noticeDao: NoticeDao,
    private val scriptHost: QuickJsScriptHost,
) {
    private val tag = "SourceRunner"

    /** 静态校验脚本（语法 + 是否定义 fetchNotices）。返回错误信息，合法时返回 null。 */
    suspend fun validateScript(script: String): String? = scriptHost.validate(script)

    /** 只抓取，不落库。[knownIds] 会交给脚本用于跳过已抓过正文的条目。 */
    suspend fun fetch(
        source: SourceEntity,
        crawlDaysGap: Int,
        knownIds: List<String> = emptyList(),
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        onLog: ((String) -> Unit)? = null,
        onProgress: ((Double, String) -> Unit)? = null,
    ): ScriptOutcome {
        val script = registry.scriptText(source)
        if (script.isBlank()) throw ScriptException("脚本内容为空")

        val sandbox = ScriptSandbox(
            allowedDomains = source.domains.toSet(),
            timeoutMs = timeoutMs,
            maxHttpCalls = MAX_HTTP_CALLS,
            maxBytes = MAX_BYTES,
        )
        return scriptHost.run(
            script,
            sandbox,
            ScriptRunParams(crawlDaysGap = crawlDaysGap, knownIds = knownIds),
            onLog = onLog,
            onProgress = onProgress,
        )
    }

    /**
     * 抓取 + 落库。失败不抛出，通过 [CrawlOutcome.error] 返回。
     */
    suspend fun crawl(
        source: SourceEntity,
        crawlDaysGap: Int,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        /** 脚本实时日志回调（抓取时展示详细进度）。 */
        onLog: ((String) -> Unit)? = null,
        /** 脚本上报的整体进度比例 0..1（用于更细粒度的进度条）。 */
        onProgress: ((Double, String) -> Unit)? = null,
    ): CrawlOutcome {
        return try {
            val existingIds = noticeDao.idsBySource(source.id).toSet()
            // 只把「已有当前格式正文」的条目视为已知：正文缺失或格式过旧的会在下次运行重抓
            val knownIds = noticeDao.idsWithContentBySource(source.id, NOTICE_CONTENT_VERSION)
            val outcome = fetch(source, crawlDaysGap, knownIds, timeoutMs, onLog, onProgress)
            val notices = outcome.notices.map { it.toFetchedNotice() }
            val ids = notices.map { it.id }

            val favoriteIds = noticeDao.selectFavoriteIds(ids)
            val notifiedIds = noticeDao.selectNotifiedIds(ids)
            val now = System.currentTimeMillis()

            // @Upsert 会整行覆盖，先记下用户态，写完再补回
            noticeDao.upsertAll(notices.map { it.toNoticeEntity(source.id, now) })
            if (favoriteIds.isNotEmpty()) noticeDao.markFavorites(favoriteIds)
            if (notifiedIds.isNotEmpty()) noticeDao.markNotified(notifiedIds)

            // 非致命问题（对齐后端 partialError）：本次结果照常入库，但把摘要写进运行结果
            val warning = buildWarning(outcome)
            sourceDao.updateResult(source.id, now, notices.size, warning)
            Logger.d(
                tag,
                "source=${source.id} fetched=${notices.size} new=${notices.count { it.id !in existingIds }} " +
                    "scanned=${outcome.scanned} skipped=${outcome.skipped} failed=${outcome.failed} " +
                    "noContent=${outcome.noContent} old=${outcome.skippedOld} " +
                    "dup=${outcome.skippedDuplicate} known=${outcome.skippedKnown}"
            )
            warning?.let { Logger.w(tag, "source=${source.id} 部分失败：$it") }

            CrawlOutcome(
                sourceId = source.id,
                sourceName = source.name,
                notices = notices,
                newNotices = notices.filter { it.id !in existingIds },
                baseline = existingIds.isEmpty(),
                error = null,
                warning = warning,
                scanned = outcome.scanned,
                skipped = outcome.skipped,
                failed = outcome.failed,
                noContent = outcome.noContent,
                restricted = outcome.restricted,
                skippedOld = outcome.skippedOld,
                skippedDuplicate = outcome.skippedDuplicate,
                skippedKnown = outcome.skippedKnown,
            )
        } catch (e: Exception) {
            Logger.e(tag, "source=${source.id} 抓取失败: ${e.message}", e)
            val message = e.localizedMessage ?: "未知错误"
            sourceDao.updateResult(source.id, System.currentTimeMillis(), 0, message)
            CrawlOutcome(source.id, source.name, emptyList(), emptyList(), false, message)
        }
    }

    /**
     * 把脚本上报的失败数与警告汇总成多行运行结果（第一行是摘要，后面每行一条警告）；
     * 没有问题时返回 null。列表页只展示第一行，属性页可以点开看全部。
     */
    private fun buildWarning(outcome: ScriptOutcome): String? {
        val lines = mutableListOf<String>()
        if (outcome.failed > 0 || outcome.noContent > 0 || outcome.skipped > 0) {
            lines += buildString {
                append("扫描 ").append(outcome.scanned).append(" 条")
                append("，跳过 ").append(outcome.skipped).append(" 条")
                append("（超出回溯窗口 ").append(outcome.skippedOld)
                append("、重复 ").append(outcome.skippedDuplicate)
                append("、已入库 ").append(outcome.skippedKnown).append("）")
                append("，入库 ").append(outcome.notices.size).append(" 条")
                if (outcome.noContent > 0) {
                    append("，其中无正文 ").append(outcome.noContent).append(" 条")
                    if (outcome.restricted > 0) {
                        append("（仅校内 ").append(outcome.restricted).append(" 条）")
                    }
                }
                if (outcome.failed > 0) append("，失败 ").append(outcome.failed).append(" 条")
            }
        }
        outcome.warnings.forEach { lines += "· $it" }
        if (lines.isEmpty()) return null
        return lines.joinToString("\n").take(MAX_WARNING_CHARS)
    }

    /**
     * 处理通知水位。
     * 首次抓取只建立基线（不打扰用户）；否则把新条目标记为已通知。
     * @return 需要发通知的条目（[notify] 为 false 时始终返回空）
     */
    suspend fun settleNotifications(outcome: CrawlOutcome, notify: Boolean): List<FetchedNotice> {
        if (outcome.notices.isEmpty()) return emptyList()

        if (outcome.baseline) {
            noticeDao.markNotified(outcome.notices.map { it.id })
            return emptyList()
        }

        if (outcome.newNotices.isEmpty()) return emptyList()
        noticeDao.markNotified(outcome.newNotices.map { it.id })
        return if (notify) outcome.newNotices else emptyList()
    }
}

private fun ScriptNotice.toFetchedNotice(): FetchedNotice = FetchedNotice(
    id = id.ifBlank { detailUrl },
    label = label.ifBlank { "资讯" },
    title = title,
    date = date,
    detailUrl = detailUrl,
    isPage = isPage,
    contentText = contentText,
    attachmentUrls = attachments,
)
