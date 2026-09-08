package org.openjwc.client.data.parser

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import org.openjwc.client.data.models.Course
import org.openjwc.client.log.Logger
import org.openjwc.client.ui.theme.courseBackgroundColors
import java.time.DayOfWeek
import kotlin.math.abs

/**
 * 解析结果封装类
 */
data class TimetableParseResult(
    val courses: List<Course>,
    val totalWeeks: Int,
    val hasWeekend: Boolean,
    val maxPeriod: Int
)

/**
 * 课表解析工具类
 *
 * 输入为规范化的课表 JSON：
 * { "name", "teacher", "location", "dayOfWeek", "startPeriod", "endPeriod", "weeks", "note" }
 * 教务系统原始字段（KCM/SKXQ/ZCMC 等）由抓取端（timetable_extractor.js）负责转换
 */
object TableParserUtils {

    private const val TAG = "TableParserUtils"

    /**
     * 字符串清理：处理空值或字符串形式的 "null"
     */
    fun String?.cleanRaw(stringToReplaceWith: String = ""): String {
        return if (this == null || this.lowercase() == "null") stringToReplaceWith else this
    }

    /**
     * 根据课程名称获取确定的颜色（同名同色）
     */
    private fun getDeterministicColor(courseName: String): Color {
        val colors = courseBackgroundColors.toList()
        if (colors.isEmpty()) return Color(0xFF6750A4)

        val index = abs(courseName.hashCode()) % colors.size
        return colors[index]
    }


    /**
     * 解析周次字符串 (例如: "1-16周", "1-10周(单)", "2,4,6周")
     */
    fun parseWeekRange(weekText: String): List<Int> {
        if (weekText.isBlank()) return emptyList()

        val parts = weekText.split(",")
        val allWeeks = mutableSetOf<Int>()

        // 正则：匹配开始周-结束周、周、(单/双)
        val regex = Regex("""(\d+)(?:-(\d+))?周?(?:\(([单双])\))?""")

        parts.forEach { part ->
            regex.find(part.trim())?.let { matchResult ->
                val start = matchResult.groupValues[1].toInt()
                val end = if (matchResult.groupValues[2].isNotEmpty()) {
                    matchResult.groupValues[2].toInt()
                } else {
                    start
                }
                val type = matchResult.groupValues.getOrNull(3)

                (start..end).forEach { w ->
                    val isMatch = when (type) {
                        "单" -> w % 2 != 0
                        "双" -> w % 2 == 0
                        else -> true
                    }
                    if (isMatch) allWeeks.add(w)
                }
            }
        }
        return allWeeks.sorted()
    }

    private fun JSONObject.firstString(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { k ->
            optString(k, "").takeIf { it.isNotBlank() && it.lowercase() != "null" }
        }

    private fun JSONObject.firstInt(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { k ->
            if (!has(k) || isNull(k)) null
            else when (val v = opt(k)) {
                is Number -> v.toInt()
                is String -> v.trim().toDoubleOrNull()?.toInt()
                else -> null
            }
        }

    /**
     * 提取周次集合：[weeks] 支持周次字符串（"1-16周(单)"）、
     * 数字数组（[1,3,5]）或字符串数组（["1-8周","10,12"]）
     */
    private fun extractWeeks(obj: JSONObject): List<Int> {
        when (val weeks = obj.opt("weeks")) {
            is JSONArray -> {
                val result = mutableSetOf<Int>()
                for (i in 0 until weeks.length()) {
                    when (val w = weeks.opt(i)) {
                        is Number -> result.add(w.toInt())
                        is String -> result.addAll(parseWeekRange(w))
                    }
                }
                if (result.isNotEmpty()) return result.sorted()
            }
            is String -> if (weeks.isNotBlank()) return parseWeekRange(weeks)
            is Number -> return listOf(weeks.toInt())
        }
        return emptyList()
    }

    /**
     * 从 JSON 数组字符串中解析完整的课表信息
     * @param jsonArrayStr 原始 JSON 数据
     * @param tableId 关联的课表元数据 ID
     */
    fun parseCoursesFromJsonArray(
        jsonArrayStr: String,
        tableId: Long
    ): TimetableParseResult {
        Logger.d(TAG, "Starting to parse JSON array for table: $tableId")

        val jsonArray = try {
            JSONArray(jsonArrayStr)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to parse JSON string: ${e.message}")
            return TimetableParseResult(emptyList(), 16, false, 13)
        }

        data class RawRow(
            val name: String?,
            val teacher: String,
            val location: String,
            val dayValue: Int,
            val start: Int,
            val end: Int,
            val weeks: List<Int>,
            val note: String
        )

        val rows = (0 until jsonArray.length())
            .mapNotNull { jsonArray.optJSONObject(it) }
            .map { obj ->
                RawRow(
                    name = obj.firstString("name"),
                    teacher = obj.firstString("teacher").cleanRaw(),
                    location = obj.firstString("location").cleanRaw(),
                    dayValue = obj.firstInt("dayOfWeek") ?: 1,
                    start = obj.firstInt("startPeriod") ?: 1,
                    end = obj.firstInt("endPeriod") ?: 0,
                    weeks = extractWeeks(obj),
                    note = obj.firstString("note").orEmpty()
                )
            }

        // 从数据推断课表全局配置
        var inferredMaxWeek = 16
        var inferredHasWeekend = false
        var inferredMaxPeriod = 13

        for (row in rows) {
            if (row.weeks.isNotEmpty()) {
                inferredMaxWeek = maxOf(inferredMaxWeek, row.weeks.max())
            }
            if (row.dayValue >= 6) {
                inferredHasWeekend = true
            }
            if (row.end > inferredMaxPeriod) {
                inferredMaxPeriod = row.end
            }
        }

        Logger.d(TAG, "Scan complete: MaxWeek=$inferredMaxWeek, Weekend=$inferredHasWeekend, MaxPeriod=$inferredMaxPeriod")

        val courses = rows.mapNotNull { row ->
            if (row.weeks.isEmpty()) {
                Logger.w(TAG, "Course [${row.name}] ignored: no valid weeks")
                return@mapNotNull null
            }
            val courseName = row.name ?: run {
                Logger.w(TAG, "Course ignored: missing 'name'")
                return@mapNotNull null
            }
            val start = if (row.start < 1) 1 else row.start
            val end = if (row.end >= start) row.end else start

            Course(
                id = 0,
                name = courseName,
                teacher = row.teacher,
                location = row.location,
                dayOfWeek = if (row.dayValue in 1..7) DayOfWeek.of(row.dayValue) else DayOfWeek.MONDAY,
                startPeriod = start,
                duration = end - start + 1,
                weekRule = row.weeks.toSet(),
                color = getDeterministicColor(courseName),
                note = row.note,
                tableId = tableId
            )
        }

        Logger.i(TAG, "Successfully parsed ${courses.size} courses.")
        return TimetableParseResult(
            courses = courses,
            totalWeeks = inferredMaxWeek,
            hasWeekend = inferredHasWeekend,
            maxPeriod = inferredMaxPeriod
        )
    }
}
