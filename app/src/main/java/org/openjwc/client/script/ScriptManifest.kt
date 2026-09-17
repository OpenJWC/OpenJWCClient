package org.openjwc.client.script

/**
 * 数据源脚本的清单，来自脚本头部的注释：
 *
 * ```
 * // @id seu-jwc
 * // @name 东南大学教务处
 * // @version 1.0.0
 * // @schedule 360
 * // @domains jwc.seu.edu.cn
 * // @labels 最新动态,教务信息
 * ```
 */
data class ScriptManifest(
    val id: String,
    val name: String,
    val version: String,
    val scheduleMinutes: Int,
    val domains: List<String>,
    val labels: List<String>,
)

object ScriptManifestParser {

    private const val DEFAULT_SCHEDULE_MINUTES = 360

    fun parse(script: String): ScriptManifest? {
        val fields = mutableMapOf<String, String>()
        for (rawLine in script.lineSequence()) {
            val line = rawLine.trim()
            if (!line.startsWith("//") && !line.startsWith("#")) continue
            val body = line.removePrefix("//").removePrefix("#").trim()
            if (!body.startsWith("@")) continue
            val space = body.indexOfFirst { it == ' ' || it == '\t' || it == ':' }
            if (space <= 0) continue
            val key = body.substring(1, space).trim().lowercase()
            val value = body.substring(space + 1).trim().trimStart(':').trim()
            if (key.isNotEmpty() && value.isNotEmpty()) fields[key] = value
        }

        val id = fields["id"] ?: return null
        return ScriptManifest(
            id = id,
            name = fields["name"] ?: id,
            version = fields["version"] ?: "1.0.0",
            scheduleMinutes = fields["schedule"]?.toIntOrNull()
                ?.coerceAtLeast(MIN_SCHEDULE_MINUTES)
                ?: DEFAULT_SCHEDULE_MINUTES,
            domains = splitList(fields["domains"]),
            labels = splitList(fields["labels"]),
        )
    }

    private fun splitList(raw: String?): List<String> =
        raw?.split(',', '，', ' ', '|')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    /** WorkManager 周期任务最短 15 分钟。 */
    const val MIN_SCHEDULE_MINUTES = 15
}
