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

        val rawObjects = mutableListOf<JSONObject>()
        var inferredMaxWeek = 16
        var inferredHasWeekend = false
        var inferredMaxPeriod = 13

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            rawObjects.add(obj)

            // 推断最大周数
            val weeks = parseWeekRange(obj.optString("ZCMC"))
            if (weeks.isNotEmpty()) {
                inferredMaxWeek = maxOf(inferredMaxWeek, weeks.maxOrNull() ?: 0)
            }

            // 推断是否包含周末课程 (SKXQ: 6=周六, 7=周日)
            if (obj.optInt("SKXQ") >= 6) {
                inferredHasWeekend = true
            }

            // 推断最大节次 (JSJC: 结束节次)
            val endPeriod = obj.optInt("JSJC")
            if (endPeriod > inferredMaxPeriod) {
                inferredMaxPeriod = endPeriod
            }
        }

        Logger.d(TAG, "Scan complete: MaxWeek=$inferredMaxWeek, Weekend=$inferredHasWeekend, MaxPeriod=$inferredMaxPeriod")

        val courses = rawObjects.mapNotNull { obj ->
            val weeks = parseWeekRange(obj.optString("ZCMC"))
            if (weeks.isEmpty()) {
                Logger.w(TAG, "Course [${obj.optString("KCM")}] ignored: No valid weeks found in '${obj.optString("ZCMC")}'")
                return@mapNotNull null
            }

            val start = obj.optInt("KSJC")
            val end = obj.optInt("JSJC")
            val courseName = obj.optString("KCM")

            val smartWeekRule = weeks.toSet()
            val dayValue = obj.optInt("SKXQ")
            val dayOfWeek = if (dayValue in 1..7) DayOfWeek.of(dayValue) else DayOfWeek.MONDAY

            Course(
                id = 0,
                name = courseName,
                teacher = obj.optString("SKJS").cleanRaw(),
                location = obj.optString("JASMC").cleanRaw(),
                dayOfWeek = dayOfWeek,
                startPeriod = start,
                duration = if (end >= start) end - start + 1 else 1,
                weekRule = smartWeekRule,
                color = getDeterministicColor(courseName),
                note = buildString {
                    append("课程号: ${obj.optString("KCH")}")
                    append("\n原始周次: ${obj.optString("ZCMC")}")
                    val groupNo = obj.optString("JXBQH").cleanRaw()
                    if (groupNo.isNotBlank()) append("\n教学班群号: $groupNo")
                },
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