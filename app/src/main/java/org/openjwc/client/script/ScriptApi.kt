package org.openjwc.client.script

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 暴露给脚本的 HTTP 桥。**同步阻塞**：QuickJS 的 evaluate 是同步的。
 */
interface ScriptHttpApi {
    fun get(url: String): String
    fun post(url: String, body: String, contentType: String): String
}

/**
 * 暴露给脚本的 HTML 解析桥（Jsoup）。所有返回值都是字符串，
 * 其中 [query] 返回 JSON 数组，脚本用 `JSON.parse` 消费。
 */
interface ScriptHtmlApi {
    fun query(html: String, selector: String): String
    fun text(html: String, selector: String): String
    fun attr(html: String, selector: String, name: String): String

    /**
     * 取第一个匹配元素的 **Markdown**（对齐 JwcCrawler 的 htmd 用法）：
     * 标题/段落/列表/表格/加粗/链接都会保留，相对链接按 [baseUrl] 转绝对。
     */
    fun markdown(html: String, selector: String, baseUrl: String): String
}

/** 通用工具。注意：QuickJS 桥不支持 Java `long`，时间戳用 Double 毫秒。 */
interface ScriptUtilApi {
    fun sha256(text: String): String
    fun resolveUrl(base: String, relative: String): String
    fun now(): Double
}

/** 日志（会走 App 的 Logger）。 */
interface ScriptConsoleApi {
    fun log(message: String)
}

/**
 * 运行结果上报（对齐后端爬虫的 progress / partialError）。
 * 警告只影响来源的「运行结果」，不会丢弃本次已抓到的资讯。
 */
interface ScriptReportApi {
    /**
     * 本次扫描统计：
     * - scanned：扫描到的行数
     * - skipped：跳过总数（= 超出回溯窗口 + 重复 + 已入库）
     * - failed：真失败（列表页不可访问、行缺日期、栏目解析不出条目）
     * - noContent：标题与链接已入库、但正文没抓到（校内限制、正文为空等）
     * - restricted：其中因「仅校内 IP 可访问」而暂缺的条数
     * - skippedOld / skippedDuplicate / skippedKnown：跳过明细
     */
    fun stats(
        scanned: Int,
        skipped: Int,
        failed: Int,
        noContent: Int,
        restricted: Int,
        skippedOld: Int,
        skippedDuplicate: Int,
        skippedKnown: Int,
    )

    /** 一条不影响整体完成的警告（最多保留 20 条）。 */
    fun warn(message: String)

    /**
     * 增量进度（每翻一页调一次）。
     * @param fraction 本次运行的整体完成比例 0..1（脚本按「栏目序号 + 当前栏目页进度」估算）。
     * @param detail 人类可读的进度描述，用于日志。
     */
    fun progress(scanned: Int, skipped: Int, failed: Int, fraction: Double, detail: String)
}

/** 运行参数（由 App 注入），脚本用它做抓取范围与去重。 */
interface ScriptParamsApi {
    /** 抓取回溯天数（「数据源」设置项，与「新鲜度高亮天数」无关）。 */
    fun crawlDaysGap(): Int

    /** 抓取截止日期，格式 `yyyy-MM-dd`；早于该日期的条目可跳过。 */
    fun crawlCutoffDate(): String

    /** 该数据源已在库中的资讯 id（JSON 数组字符串），可用于跳过重复抓正文。 */
    fun knownIdsJson(): String
}

/** 一次脚本运行的参数。 */
data class ScriptRunParams(
    val crawlDaysGap: Int = 200,
    val knownIds: List<String> = emptyList(),
)

/** 脚本返回的单条资讯。字段名与脚本契约一致。 */
@Serializable
data class ScriptNotice(
    val id: String = "",
    val label: String = "",
    val title: String = "",
    val date: String = "",
    @SerialName("detail_url") val detailUrl: String = "",
    @SerialName("is_page") val isPage: Boolean = true,
    @SerialName("content_text") val contentText: String? = null,
    val attachments: List<String>? = null,
)

/** 一次脚本运行的完整结果。 */
data class ScriptOutcome(
    val notices: List<ScriptNotice> = emptyList(),
    val scanned: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    /** 已入库但正文没抓到的条目数（校内限制、正文为空等）。 */
    val noContent: Int = 0,
    /** 其中因「仅校内 IP 可访问」而暂缺的条数。 */
    val restricted: Int = 0,
    val skippedOld: Int = 0,
    val skippedDuplicate: Int = 0,
    val skippedKnown: Int = 0,
    val warnings: List<String> = emptyList(),
)

/** 脚本执行超时。 */
class ScriptTimeoutException(message: String = "脚本执行超时") : RuntimeException(message)

/** 脚本违反沙箱限制（域名白名单 / 调用次数）。 */
class ScriptSandboxException(message: String) : RuntimeException(message)

/** 脚本执行失败。 */
class ScriptException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
