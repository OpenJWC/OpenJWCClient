package org.openjwc.client.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.openjwc.client.data.models.NoticeEntity
import org.openjwc.client.data.repository.DailyReportSource
import org.openjwc.client.data.repository.NoticeCorpus
import org.openjwc.client.data.repository.TimetableSnapshot
import org.openjwc.client.data.repository.TimetableSource
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** 工具执行失败（会作为失败观察交给模型继续检索，不等于整轮失败）。 */
class AgentToolException(val code: String, message: String) : RuntimeException(message)

/**
 * 本地资讯工具集（结构化工具，不经过 Shell）。
 *
 * 所有工具都是只读的，只能访问本地 `notices` 语料；
 * 返回文本会被 AgentLoop 按字节预算截断后再交给模型。
 */
class AgentTools(
    private val repository: NoticeCorpus,
    /** 课表读取能力；为 null 时不暴露 get_timetable 工具。 */
    private val timetable: TimetableSource? = null,
    /** 日报只读能力；为 null 时不暴露 get_daily_report 工具。 */
    private val dailyReport: DailyReportSource? = null,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    companion object {
        private val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

        const val TOOL_SEARCH = "search_notices"
        const val TOOL_READ = "read_notice"
        const val TOOL_LABELS = "list_labels"
        const val TOOL_SOURCES = "list_sources"
        const val TOOL_TIME = "current_time"
        const val TOOL_SOURCE_STATUS = "get_source_status"
        const val TOOL_DAILY_REPORT = "get_daily_report"
        const val TOOL_TIMETABLE = "get_timetable"
        const val TOOL_TIMETABLES = "list_timetables"
        const val TOOL_COURSES_ON = "get_courses_on"
        const val TOOL_FIND_COURSE = "find_course"

        /** 每页条数，与后端 VFS 一致。 */
        const val PAGE_SIZE = 20

        private const val MAX_READ_CHARS = 12_000
        private const val DEFAULT_READ_CHARS = 6_000
        private const val MAX_TITLE_CHARS = 200
        private const val MAX_QUERY_CHARS = 200
        private const val MAX_LABEL_CHARS = 100
        private const val MAX_SUMMARY_CHARS = 600

        private val WHITESPACE = Regex("\\s+")

        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        /** 工具的中文名（UI 展示用，不暴露英文工具名）。 */
        fun displayName(name: String): String = when (name) {
            TOOL_SEARCH -> "检索资讯"
            TOOL_READ -> "阅读资讯"
            TOOL_LABELS -> "查看栏目"
            TOOL_SOURCES -> "查看数据源"
            TOOL_TIME -> "查看当前时间"
            TOOL_SOURCE_STATUS -> "查看数据源状态"
            TOOL_DAILY_REPORT -> "查看日报"
            TOOL_TIMETABLES -> "查看课表列表"
            TOOL_TIMETABLE -> "查看课表"
            TOOL_COURSES_ON -> "查看某天课程"
            TOOL_FIND_COURSE -> "查找课程"
            else -> "工具"
        }
    }

    private val noticeSpecs: List<AgentToolSpec> = listOf(
        AgentToolSpec(
            name = TOOL_SEARCH,
            description = "检索本地资讯库：按关键词（同时匹配标题与正文）、栏目、数据源、日期范围筛选并分页。" +
                "关键词可以是人名、老师、课程名、活动名、机构名等任意词。" +
                "返回元数据（ID、日期、来源、栏目、标题），带关键词时**还会给出命中位置与正文片段**，" +
                "可以直接据此判断相关性，不必逐条读正文。需要完整正文时再用 read_notice。",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("query") {
                        put("type", "string")
                        put("description", "关键词，会匹配标题与正文；留空表示只按条件列举")
                    }
                    putJsonObject("label") {
                        put("type", "string")
                        put("description", "栏目名，可用 list_labels 查看")
                    }
                    putJsonObject("source_id") {
                        put("type", "string")
                        put("description", "数据源 id 或名称，可用 list_sources 查看；留空表示不限")
                    }
                    putJsonObject("from") {
                        put("type", "string")
                        put("description", "起始日期（含），格式 yyyy-MM-dd")
                    }
                    putJsonObject("to") {
                        put("type", "string")
                        put("description", "结束日期（含），格式 yyyy-MM-dd")
                    }
                    putJsonObject("sort") {
                        put("type", "string")
                        put("enum", buildJsonArray {
                            add(JsonPrimitive("newest"))
                            add(JsonPrimitive("relevance"))
                        })
                        put("description", "排序方式，默认 newest（最新优先）")
                    }
                    putJsonObject("page") {
                        put("type", "integer")
                        put("minimum", 1)
                        put("maximum", 50)
                        put("description", "页码，每页 $PAGE_SIZE 条，默认 1")
                    }
                    putJsonObject("favorite") {
                        put("type", "boolean")
                        put("description", "true 表示只看已收藏的资讯")
                    }
                }
                put("additionalProperties", false)
            },
        ),
        AgentToolSpec(
            name = TOOL_READ,
            description = "读取一条资讯的正文（可按字符偏移续读；也可用 keyword 只取命中段落）。" +
                "正文含发布日期、官方链接与附件链接，附件内容未下载。",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("id") {
                        put("type", "string")
                        put("description", "资讯 ID（search_notices 返回）")
                    }
                    putJsonObject("offset") {
                        put("type", "integer")
                        put("minimum", 0)
                        put("description", "字符偏移，默认 0")
                    }
                    putJsonObject("limit") {
                        put("type", "integer")
                        put("minimum", 1)
                        put("maximum", MAX_READ_CHARS)
                        put("description", "读取字符数，默认 $DEFAULT_READ_CHARS")
                    }
                    putJsonObject("keyword") {
                        put("type", "string")
                        put("description", "只在正文里找这个词，返回命中段落（最多 5 段）而不是整篇，省上下文")
                    }
                }
                put("required", buildJsonArray { add(JsonPrimitive("id")) })
                put("additionalProperties", false)
            },
        ),
        AgentToolSpec(
            name = TOOL_LABELS,
            description = "列出本地资讯库的全部栏目及其条数。",
            parameters = emptyParameters(),
        ),
        AgentToolSpec(
            name = TOOL_SOURCES,
            description = "列出已订阅的数据源（id、名称、栏目、最近抓取时间）。",
            parameters = emptyParameters(),
        ),
        AgentToolSpec(
            name = TOOL_TIME,
            description = "返回当前日期时间（含时区），用于理解“今天/昨天”等自然日。",
            parameters = emptyParameters(),
        ),
        AgentToolSpec(
            name = TOOL_SOURCE_STATUS,
            description = "查看已订阅数据源的运行状态：最近抓取时间、最近新增条数、抓取周期，以及最近一次失败原因。" +
                "用户问“某个源怎么没更新/抓取失败了”时使用。",
            parameters = emptyParameters(),
        ),
    )

    private val dailyReportSpec = AgentToolSpec(
        name = TOOL_DAILY_REPORT,
        description = "读取本地已生成的日报（按天汇总的资讯摘要）。day 为 yyyy-MM-dd，留空表示今天；" +
            "只有已生成完成的日报才有内容，没有时如实说明。",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("day") {
                    put("type", "string")
                    put("description", "日期 yyyy-MM-dd，留空表示今天")
                }
            }
            put("additionalProperties", false)
        },
    )

    private val timetableSpecs: List<AgentToolSpec> = listOf(
        AgentToolSpec(
            name = TOOL_TIMETABLES,
            description = "列出用户本机保存的全部课表（ID、名称、学期起始、总周数、当前周、课程数），" +
                "用于确认有哪些课表可选。",
            parameters = emptyParameters(),
        ),
        AgentToolSpec(
            name = TOOL_TIMETABLE,
            description = "读取某张课表的课程：课程名、教师、地点、星期、节次与上课周次。" +
                "回答“今天/明天/这周有什么课”“某门课在哪上”等问题时使用；用户没有课表时会如实说明。",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("table") {
                        put("type", "string")
                        put("description", "课表名或 ID（可用 list_timetables 查看）；留空表示当前正在使用的那张")
                    }
                    putJsonObject("day") {
                        put("type", "integer")
                        put("minimum", 1)
                        put("maximum", 7)
                        put("description", "只看星期几（1=周一 … 7=周日）；留空返回整周")
                    }
                }
                put("additionalProperties", false)
            },
        ),
        AgentToolSpec(
            name = TOOL_COURSES_ON,
            description = "查看某一天有哪些课（含节次、地点、教师、周次）。" +
                "回答“今天/明天/某天有什么课”时优先用它；date 可为 today / tomorrow / yyyy-MM-dd。" +
                "table 留空表示在所有课表中查找（按各自学期自动匹配该日期并标明来源），填课表名/ID 则只查那一张。",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("date") {
                        put("type", "string")
                        put("description", "today / tomorrow / yyyy-MM-dd，留空表示今天")
                    }
                    putJsonObject("table") {
                        put("type", "string")
                        put("description", "课表名或 ID；留空表示在所有课表中查找")
                    }
                }
                put("additionalProperties", false)
            },
        ),
        AgentToolSpec(
            name = TOOL_FIND_COURSE,
            description = "在课表里按课程名、教师或地点查找课程，返回星期、节次、地点与周次。" +
                "table 留空表示在所有课表中查找并标明来源，填课表名/ID 则只查那一张。",
            parameters = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("query") {
                        put("type", "string")
                        put("description", "课程名 / 教师 / 地点关键词")
                    }
                    putJsonObject("table") {
                        put("type", "string")
                        put("description", "课表名或 ID；留空表示在所有课表中查找")
                    }
                }
                put("required", buildJsonArray { add(JsonPrimitive("query")) })
                put("additionalProperties", false)
            },
        ),
    )

    /** 实际暴露给模型的工具（没有课表时不暴露课表工具，没有日报时不暴露日报工具）。 */
    val specs: List<AgentToolSpec> = buildList {
        addAll(noticeSpecs)
        if (dailyReport != null) add(dailyReportSpec)
        if (timetable != null) addAll(timetableSpecs)
    }

    private fun emptyParameters(): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {}
        put("additionalProperties", false)
    }

    /** 工具名是否受支持。 */
    fun supports(name: String): Boolean = specs.any { it.name == name }

    /** 工具指向的本地对象 id（目前只有 read_notice 的资讯 id），用于 UI 跳转。 */
    fun targetId(name: String, arguments: String): String? {
        if (name != TOOL_READ) return null
        val obj = runCatching { json.parseToJsonElement(arguments).jsonObject }.getOrNull() ?: return null
        return obj.string("id").trim().takeIf { it.isNotBlank() }
    }

    /** 逐行列出所有参数的人类可读明细，用于工具卡片展示。 */
    suspend fun summarize(name: String, arguments: String): String {
        val obj = runCatching { json.parseToJsonElement(arguments).jsonObject }.getOrNull()
        fun str(key: String) = obj?.string(key).orEmpty()
        fun num(key: String) = obj?.int(key)
        fun flag(key: String) = obj?.bool(key) ?: false

        val lines = mutableListOf<String>()
        fun add(label: String, value: String?) {
            if (!value.isNullOrBlank()) lines += "$label：$value"
        }

        when (name) {
            TOOL_SEARCH -> {
                add("关键词", str("query"))
                add("栏目", str("label"))
                val sourceRaw = str("source_id")
                if (sourceRaw.isNotBlank()) {
                    val name = runCatching { repository.subscribedSources() }
                        .getOrDefault(emptyList())
                        .firstOrNull { it.id == sourceRaw }?.name
                    add("数据源", name ?: sourceRaw)
                }
                val from = str("from")
                val to = str("to")
                if (from.isNotBlank() || to.isNotBlank()) {
                    add("日期", from.ifBlank { "不限" } + " ~ " + to.ifBlank { "不限" })
                }
                add("排序", when (str("sort")) {
                    "relevance" -> "相关度优先"
                    "newest" -> "最新优先"
                    else -> null
                })
                add("页码", num("page")?.toString())
                if (flag("favorite")) add("范围", "仅已收藏")
                if (lines.isEmpty()) lines += "列出资讯（无筛选条件）"
            }

            TOOL_READ -> {
                val id = str("id")
                if (id.isNotBlank()) {
                    val title = runCatching { repository.findNotice(id) }.getOrNull()?.title
                    add("资讯", title?.takeIf { it.isNotBlank() } ?: "（本地未找到该资讯）")
                }
                add("只取关键词", str("keyword"))
                add("字符偏移", num("offset")?.toString())
                add("读取字符数", num("limit")?.toString())
            }

            TOOL_DAILY_REPORT -> add("日期", str("day").ifBlank { "今天" })

            TOOL_TIMETABLE -> {
                add("课表", str("table").ifBlank { "当前课表" })
                add("星期", num("day")?.let { "周" + ("一二三四五六日".getOrNull(it - 1) ?: it.toString()) })
            }

            TOOL_COURSES_ON -> {
                add("日期", str("date").ifBlank { "今天" })
                add("课表", str("table").ifBlank { "所有课表" })
            }

            TOOL_FIND_COURSE -> {
                add("关键词", str("query"))
                add("课表", str("table").ifBlank { "所有课表" })
            }

            TOOL_LABELS -> lines += "列出全部栏目及条数"
            TOOL_SOURCES -> lines += "列出已订阅的数据源"
            TOOL_TIME -> lines += "获取当前日期时间"
            TOOL_SOURCE_STATUS -> lines += "查看各数据源的运行状态"
            TOOL_TIMETABLES -> lines += "列出本机保存的全部课表"
            else -> lines += "执行工具"
        }
        return lines.joinToString("\n").take(MAX_SUMMARY_CHARS)
    }

    /** 执行工具；失败时抛出 [AgentToolException]。 */
    suspend fun execute(name: String, arguments: String): String {
        val args = runCatching { json.parseToJsonElement(arguments).jsonObject }.getOrElse {
            throw AgentToolException("tool_invalid_arguments", "工具参数不是合法 JSON")
        }
        return when (name) {
            TOOL_SEARCH -> search(args)
            TOOL_READ -> read(args)
            TOOL_LABELS -> listLabels()
            TOOL_SOURCES -> listSources()
            TOOL_TIME -> currentTime()
            TOOL_SOURCE_STATUS -> sourceStatus()
            TOOL_DAILY_REPORT -> readDailyReport(args)
            TOOL_TIMETABLES -> listTimetables()
            TOOL_TIMETABLE -> readTimetable(args)
            TOOL_COURSES_ON -> coursesOn(args)
            TOOL_FIND_COURSE -> findCourse(args)
            else -> throw AgentToolException("tool_unsupported", "未知工具: $name")
        }
    }

    private suspend fun search(args: JsonObject): String {
        val query = args.string("query").trim().take(MAX_QUERY_CHARS)
        val label = args.string("label").trim().take(MAX_LABEL_CHARS)
        val sourceNames = sourceNames()
        val sourceId = resolveSourceId(args.string("source_id").trim(), sourceNames)
        val from = args.string("from").trim().also { validateDay(it, "from") }
        val to = args.string("to").trim().also { validateDay(it, "to") }
        if (from.isNotEmpty() && to.isNotEmpty() && from > to) {
            throw AgentToolException("tool_invalid_arguments", "from 不能晚于 to")
        }
        val relevance = args.string("sort").trim() == "relevance"
        val page = (args.int("page") ?: 1).coerceIn(1, 50)
        val favoriteOnly = args.bool("favorite")

        val total = repository.countNotices(query, label, sourceId, from, to, favoriteOnly)
        val items = repository.searchNotices(
            query = query,
            label = label,
            sourceId = sourceId,
            fromDay = from,
            toDay = to,
            favoriteOnly = favoriteOnly,
            relevance = relevance,
            limit = PAGE_SIZE,
            offset = (page - 1) * PAGE_SIZE,
        )
        val pages = maxOf(1, (total + PAGE_SIZE - 1) / PAGE_SIZE)
        val header = buildString {
            append("共 ").append(total).append(" 条；第 ").append(page).append('/').append(pages)
            append(" 页；排序 ").append(if (relevance) "relevance" else "newest")
            append("。")
            if (items.isEmpty()) append("没有结果，请换关键词、拆分词语或扩大日期范围。")
        }
        if (items.isEmpty()) return header
        return listing(items, sourceNames, query) + "\n" + header
    }

    /** 数据源 id → 名称（含已订阅源）。 */
    private suspend fun sourceNames(): Map<String, String> =
        repository.subscribedSources().associate { it.id to it.name }

    /** 允许模型传数据源 id 或名称；无法识别时按未知处理。 */
    private fun resolveSourceId(raw: String, names: Map<String, String>): String? {
        if (raw.isBlank()) return null
        if (names.containsKey(raw)) return raw
        return names.entries.firstOrNull { it.value == raw }?.key ?: raw
    }

    private fun sourceLabel(sourceId: String?, names: Map<String, String>): String {
        if (sourceId.isNullOrBlank()) return "未知来源"
        return names[sourceId] ?: sourceId
    }

    private suspend fun read(args: JsonObject): String {
        val id = args.string("id").trim()
        val keyword = args.string("keyword").trim().take(MAX_QUERY_CHARS)
        if (id.isEmpty() || id.length > 256) {
            throw AgentToolException("tool_invalid_arguments", "资讯 ID 无效")
        }
        val offset = (args.int("offset") ?: 0).coerceAtLeast(0)
        val limit = (args.int("limit") ?: DEFAULT_READ_CHARS).coerceIn(1, MAX_READ_CHARS)
        val notice = repository.findNotice(id)
            ?: throw AgentToolException("tool_not_found", "资讯不存在: $id")
        val sourceNames = sourceNames()

        val body = buildString {
            append("ID: ").append(notice.id).append('\n')
            append("标题: ").append(notice.title).append('\n')
            append("来源: ").append(sourceLabel(notice.sourceId, sourceNames)).append('\n')
            append("栏目: ").append(notice.label).append('\n')
            append("发布日期: ").append(notice.publishedDay).append('\n')
            append("官方链接: ").append(notice.detailUrl).append('\n')
            append("\n")
            append(notice.content.orEmpty())
            append("\n\n附件链接（未下载或解析内容）：\n")
            notice.attachments.orEmpty().forEach { append("- ").append(it).append('\n') }
        }
        // 带关键词：只回命中段落（等价于在单篇里 grep）
        if (keyword.isNotEmpty()) {
            val hits = passages(notice.content.orEmpty(), keyword)
            val header = body.substringBefore("\n\n").trimEnd()
            return if (hits.isEmpty()) {
                "$header\n\n正文里没有出现「$keyword」。"
            } else {
                "$header\n\n命中「$keyword」的段落：\n" + hits.joinToString("\n---\n")
            }
        }

        val chars = body.toCharArray()
        if (offset >= chars.size) {
            throw AgentToolException("tool_invalid_arguments", "字符偏移超过总长度 ${chars.size}")
        }
        val end = minOf(chars.size, offset + limit)
        val slice = String(chars, offset, end - offset)
        val suffix = buildString {
            append("\n[字符 ").append(offset).append(':').append(end).append(" / ").append(chars.size).append(']')
            if (end < chars.size) {
                append("\n续读: read_notice(id=\"").append(notice.id).append("\", offset=").append(end)
                    .append(", limit=").append(limit).append(")")
            } else {
                append(" EOF")
            }
        }
        return slice + suffix
    }

    private suspend fun listLabels(): String {
        val labels = repository.corpusLabels()
        if (labels.isEmpty()) return "本地资讯库为空。"
        return buildString {
            append("共 ").append(labels.size).append(" 个栏目：\n")
            labels.forEach { (label, count) -> append("- ").append(label).append("（").append(count).append(" 条）\n") }
        }
    }

    private suspend fun listSources(): String {
        val sources = repository.subscribedSources()
        if (sources.isEmpty()) return "没有已订阅的数据源。"
        return buildString {
            append("已订阅 ").append(sources.size).append(" 个数据源：\n")
            sources.forEach { source ->
                append("- id=").append(source.id).append(" 名称=").append(source.name)
                if (source.labels.isNotEmpty()) append(" 栏目=").append(source.labels.joinToString("、"))
                source.lastRunAt?.let { append(" 最近抓取=").append(formatInstant(it)) }
                append('\n')
            }
        }
    }

    private fun currentTime(): String = ZonedDateTime.now(zoneId).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    /** 已订阅数据源的运行状态（最近抓取/新增/失败）。 */
    private suspend fun sourceStatus(): String {
        val sources = repository.subscribedSources()
        if (sources.isEmpty()) return "没有已订阅的数据源。"
        return buildString {
            append("已订阅 ").append(sources.size).append(" 个数据源：\n")
            sources.forEach { source ->
                append("- ").append(source.name).append("（").append(source.id).append("）")
                append(" 周期 ").append(source.scheduleMinutes).append(" 分钟")
                append("；最近抓取 ").append(source.lastRunAt?.let { formatInstant(it) } ?: "从未")
                append("；最近新增 ").append(source.lastCount).append(" 条")
                source.lastError?.let { error ->
                    append("；最近失败：").append(error.lineSequence().first())
                }
                append('\n')
            }
        }.trimEnd()
    }

    /** 读取某日已生成的日报正文。 */
    private suspend fun readDailyReport(args: JsonObject): String {
        val source = dailyReport
            ?: throw AgentToolException("tool_unsupported", "当前没有可读取的日报")
        val raw = args.string("day").trim()
        val day = if (raw.isEmpty()) LocalDate.now(zoneId).toString() else raw
        runCatching { LocalDate.parse(day, DATE_FORMAT) }.getOrElse {
            throw AgentToolException("tool_invalid_arguments", "day 日期格式应为 yyyy-MM-dd")
        }
        val content = source.completedReport(day)
        return if (content.isNullOrBlank()) {
            "该日（$day）还没有已生成的日报。可以让用户在「日报」页生成，或改用 search_notices 检索当日资讯。"
        } else {
            "日报 $day：\n\n$content"
        }
    }

    /** 列出本机全部课表。 */
    private suspend fun listTimetables(): String {
        val source = timetable
            ?: throw AgentToolException("tool_unsupported", "当前没有可读取的课表")
        val all = source.timetables()
        if (all.isEmpty()) return "用户还没有配置课表。"
        return buildString {
            append("共 ").append(all.size).append(" 张课表（ID、名称、学期起始、总周数、当前周、课程数）：\n")
            all.forEach { snapshot ->
                append("- ").append(snapshot.id).append(' ').append(snapshot.name)
                append("  起始 ").append(snapshot.startDate)
                append("，共 ").append(snapshot.totalWeeks).append(" 周")
                append("，").append(
                    snapshot.currentWeek?.let { "当前第 $it 周" } ?: "不在学期内"
                )
                append("，课程 ").append(snapshot.courses.size).append(" 门")
                if (snapshot.isCurrent) append("（当前使用）")
                append('\n')
            }
        }.trimEnd()
    }

    /** 读取课表，按星期分组输出（可指定课表与星期）。 */
    private suspend fun readTimetable(args: JsonObject): String {
        val source = timetable
            ?: throw AgentToolException("tool_unsupported", "当前没有可读取的课表")
        val key = args.string("table").trim()
        val snapshot = if (key.isEmpty()) {
            source.currentTimetable()
        } else {
            val all = source.timetables()
            all.firstOrNull { it.id.toString() == key }
                ?: all.firstOrNull { it.name == key }
                ?: all.firstOrNull { it.name.contains(key, ignoreCase = true) }
        } ?: return if (key.isEmpty()) {
            "用户还没有配置课表。"
        } else {
            "没有找到名为「$key」的课表，可以先用 list_timetables 查看有哪些课表。"
        }
        val onlyDay = args.int("day")?.coerceIn(1, 7)
        val today = LocalDate.now(zoneId)

        return buildString {
            append("课表：").append(snapshot.name).append('\n')
            append("学期起始：").append(snapshot.startDate)
            append("，共 ").append(snapshot.totalWeeks).append(" 周；")
            append(
                snapshot.currentWeek?.let { "当前第 $it 周" } ?: "当前不在学期内"
            ).append('\n')
            append("今天：").append(today).append("（")
                .append(WEEKDAY_NAMES[today.dayOfWeek.value - 1]).append("）\n")

            if (snapshot.courses.isEmpty()) {
                append("\n课表里还没有课程。")
                return@buildString
            }

            val days = if (onlyDay != null) listOf(onlyDay) else (1..7)
            for (day in days) {
                val ofDay = snapshot.courses
                    .filter { it.dayOfWeek == day }
                    .sortedBy { it.startPeriod }
                if (ofDay.isEmpty()) continue
                append('\n').append(WEEKDAY_NAMES[day - 1]).append('\n')
                ofDay.forEach { course ->
                    append("  第 ").append(course.startPeriod)
                    if (course.duration > 1) {
                        append('-').append(course.startPeriod + course.duration - 1)
                    }
                    append(" 节  ").append(course.name)
                    if (course.location.isNotBlank()) append("  ").append(course.location)
                    if (course.teacher.isNotBlank()) append("  ").append(course.teacher)
                    append("  ").append(formatWeeks(course.weeks, snapshot.totalWeeks))
                    if (course.note.isNotBlank()) append("  备注：").append(course.note)
                    append('\n')
                }
            }
        }.trimEnd()
    }

    /** 某天有哪些课。table 留空时在所有课表里按各自学期匹配该日期。 */
    private suspend fun coursesOn(args: JsonObject): String {
        val source = timetable
            ?: throw AgentToolException("tool_unsupported", "当前没有可读取的课表")
        val key = args.string("table").trim()
        val snapshots = if (key.isEmpty()) {
            source.timetables().ifEmpty {
                throw AgentToolException("tool_not_found", "用户还没有配置课表")
            }
        } else {
            listOf(resolveTimetable(key))
        }
        val date = parseDayArg(args.string("date").trim())
        val weekday = WEEKDAY_NAMES[date.dayOfWeek.value - 1]
        val showTable = key.isNotEmpty() || snapshots.size > 1

        val blocks = mutableListOf<String>()
        val outOfSemester = mutableListOf<String>()
        for (snapshot in snapshots) {
            val week = weekOf(snapshot, date)
            if (week == null) {
                outOfSemester += snapshot.name
                continue
            }
            val courses = snapshot.courses
                .filter { it.dayOfWeek == date.dayOfWeek.value && it.weeks.contains(week) }
                .sortedBy { it.startPeriod }
            if (courses.isEmpty()) continue
            blocks += buildString {
                if (showTable) append("课表「").append(snapshot.name).append("」")
                append("第 ").append(week).append(" 周：\n")
                courses.forEach { course ->
                    append("  - 第 ").append(course.startPeriod)
                    if (course.duration > 1) append('-').append(course.startPeriod + course.duration - 1)
                    append(" 节  ").append(course.name)
                    if (course.location.isNotBlank()) append("  ").append(course.location)
                    if (course.teacher.isNotBlank()) append("  ").append(course.teacher)
                    append('\n')
                }
            }.trimEnd()
        }

        val head = "$date（$weekday）"
        if (blocks.isEmpty()) {
            return if (outOfSemester.isEmpty()) {
                "$head 没有课。"
            } else {
                "$head 没有课（不在学期内的课表：${outOfSemester.joinToString("、")}）。"
            }
        }
        return head + "：\n" + blocks.joinToString("\n")
    }

    /** 按课程名/教师/地点查找课程。table 留空时在所有课表里查并标明来源。 */
    private suspend fun findCourse(args: JsonObject): String {
        val source = timetable
            ?: throw AgentToolException("tool_unsupported", "当前没有可读取的课表")
        val query = args.string("query").trim().take(MAX_QUERY_CHARS)
        if (query.isEmpty()) throw AgentToolException("tool_invalid_arguments", "query 不能为空")
        val key = args.string("table").trim()
        val snapshots = if (key.isEmpty()) {
            source.timetables().ifEmpty {
                throw AgentToolException("tool_not_found", "用户还没有配置课表")
            }
        } else {
            listOf(resolveTimetable(key))
        }
        val needle = query.lowercase()
        val multi = snapshots.size > 1
        val lines = mutableListOf<String>()
        snapshots.forEach { snapshot ->
            snapshot.courses
                .filter {
                    it.name.lowercase().contains(needle) ||
                        it.teacher.lowercase().contains(needle) ||
                        it.location.lowercase().contains(needle)
                }
                .sortedWith(compareBy({ it.dayOfWeek }, { it.startPeriod }))
                .forEach { course ->
                    lines += buildString {
                        append("- ")
                        if (multi) append("「").append(snapshot.name).append("」")
                        append(WEEKDAY_NAMES[course.dayOfWeek - 1])
                        append(" 第 ").append(course.startPeriod)
                        if (course.duration > 1) append('-').append(course.startPeriod + course.duration - 1)
                        append(" 节  ").append(course.name)
                        if (course.location.isNotBlank()) append("  ").append(course.location)
                        if (course.teacher.isNotBlank()) append("  ").append(course.teacher)
                        append("  ").append(formatWeeks(course.weeks, snapshot.totalWeeks))
                    }
                }
        }
        if (lines.isEmpty()) {
            return if (multi) {
                "所有课表里都没有匹配「$query」的课程。"
            } else {
                "课表「${snapshots.first().name}」里没有匹配「$query」的课程。"
            }
        }
        val header = if (multi) {
            "在所有课表里匹配「$query」的课程："
        } else {
            "课表「${snapshots.first().name}」匹配「$query」的课程："
        }
        return header + "\n" + lines.joinToString("\n")
    }

    /** 解析课表：空 key 用当前课表，否则按 ID / 名称 / 名称包含匹配。 */
    private suspend fun resolveTimetable(key: String): TimetableSnapshot {
        val source = timetable
            ?: throw AgentToolException("tool_unsupported", "当前没有可读取的课表")
        if (key.isEmpty()) {
            return source.currentTimetable()
                ?: throw AgentToolException("tool_not_found", "用户还没有配置课表")
        }
        val all = source.timetables()
        return all.firstOrNull { it.id.toString() == key }
            ?: all.firstOrNull { it.name == key }
            ?: all.firstOrNull { it.name.contains(key, ignoreCase = true) }
            ?: throw AgentToolException("tool_not_found", "没有找到名为「$key」的课表")
    }

    /** date 参数：today / tomorrow / yesterday / yyyy-MM-dd，空串按今天。 */
    private fun parseDayArg(raw: String): LocalDate = when (raw.lowercase()) {
        "", "today", "今天" -> LocalDate.now(zoneId)
        "tomorrow", "明天" -> LocalDate.now(zoneId).plusDays(1)
        "yesterday", "昨天" -> LocalDate.now(zoneId).minusDays(1)
        else -> runCatching { LocalDate.parse(raw, DATE_FORMAT) }.getOrElse {
            throw AgentToolException("tool_invalid_arguments", "日期格式应为 yyyy-MM-dd（或 today/tomorrow）")
        }
    }

    /** 某日期是学期第几周；不在学期内返回 null。 */
    private fun weekOf(snapshot: TimetableSnapshot, date: LocalDate): Int? {
        if (snapshot.totalWeeks <= 0) return null
        val startMonday = runCatching { LocalDate.parse(snapshot.startDate, DATE_FORMAT) }.getOrNull()
            ?.with(DayOfWeek.MONDAY) ?: return null
        val endSunday = startMonday.plusWeeks(snapshot.totalWeeks.toLong()).minusDays(1)
        if (date.isBefore(startMonday) || date.isAfter(endSunday)) return null
        val days = ChronoUnit.DAYS.between(startMonday, date)
        return ((days / 7).toInt() + 1).coerceIn(1, snapshot.totalWeeks)
    }

    /** 周次的人类可读描述：每周 / 单周 / 双周 / 合并区间。 */
    private fun formatWeeks(weeks: Set<Int>, totalWeeks: Int): String {
        if (weeks.isEmpty()) return "周次未指定"
        val all = (1..totalWeeks).toSet()
        if (weeks.containsAll(all)) return "每周"
        if (weeks == all.filter { it % 2 == 1 }.toSet()) return "单周"
        if (weeks == all.filter { it % 2 == 0 }.toSet()) return "双周"
        val sorted = weeks.sorted()
        val segments = mutableListOf<String>()
        var i = 0
        while (i < sorted.size) {
            val start = sorted[i]
            var end = start
            while (i + 1 < sorted.size && sorted[i + 1] == end + 1) end = sorted[++i]
            segments += if (start == end) "$start" else "$start-$end"
            i++
        }
        return segments.joinToString("、") + " 周"
    }

    private fun formatInstant(epochMillis: Long): String =
        java.time.Instant.ofEpochMilli(epochMillis).atZone(zoneId)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    /**
     * 制表符分隔：ID、日期、来源、栏目、标题；带关键词时再追加「命中位置 + 片段」，
     * 让模型不必逐条 read_notice 就能判断相关性（等价于 grep 的上下文输出）。
     */
    private fun listing(
        items: List<NoticeEntity>,
        names: Map<String, String>,
        query: String = "",
    ): String = buildString {
        items.forEach { notice ->
            append(notice.id).append('\t')
                .append(notice.publishedDay).append('\t')
                .append(sourceLabel(notice.sourceId, names)).append('\t')
                .append(notice.label.take(MAX_LABEL_CHARS)).append('\t')
                .append(notice.title.take(MAX_TITLE_CHARS))
            if (query.isNotEmpty()) {
                matchInfo(notice, query)?.let { append('\t').append(it) }
            }
            append('\n')
        }
    }.trimEnd()

    /** 「命中位置｜片段」，例如 `正文｜…本次考试报名时间…`；未命中返回 null。 */
    private fun matchInfo(notice: NoticeEntity, query: String): String? {
        val inTitle = notice.title.contains(query, ignoreCase = true)
        val snippet = snippet(notice.content, query)
        if (!inTitle && snippet == null) return null
        val where = when {
            inTitle && snippet != null -> "标题+正文"
            inTitle -> "标题"
            else -> "正文"
        }
        return if (snippet != null) "$where｜$snippet" else where
    }

    /** 关键词附近的片段（折叠空白、两端补省略号）。 */
    private fun snippet(content: String?, query: String, radius: Int = 40): String? {
        if (content.isNullOrBlank() || query.isBlank()) return null
        val index = content.indexOf(query, ignoreCase = true)
        if (index < 0) return null
        val start = (index - radius).coerceAtLeast(0)
        val end = (index + query.length + radius).coerceAtMost(content.length)
        return buildString {
            if (start > 0) append('…')
            append(content.substring(start, end).replace(WHITESPACE, " ").trim())
            if (end < content.length) append('…')
        }
    }

    /** 正文里关键词出现的所有位置（最多 [max] 段）。 */
    private fun passages(content: String, query: String, max: Int = 5, radius: Int = 120): List<String> {
        if (content.isBlank() || query.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        var from = 0
        while (result.size < max) {
            val index = content.indexOf(query, from, ignoreCase = true)
            if (index < 0) break
            val start = (index - radius).coerceAtLeast(0)
            val end = (index + query.length + radius).coerceAtMost(content.length)
            result += buildString {
                append("[字符 ").append(start).append(':').append(end).append("] ")
                if (start > 0) append('…')
                append(content.substring(start, end).replace(WHITESPACE, " ").trim())
                if (end < content.length) append('…')
            }
            from = index + query.length
        }
        return result
    }

    private fun validateDay(value: String, field: String) {
        if (value.isEmpty()) return
        runCatching { java.time.LocalDate.parse(value, DATE_FORMAT) }.getOrElse {
            throw AgentToolException("tool_invalid_arguments", "$field 日期格式应为 yyyy-MM-dd")
        }
    }

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

    private fun JsonObject.int(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.bool(key: String): Boolean =
        this[key]?.jsonPrimitive?.booleanOrNull ?: false
}
