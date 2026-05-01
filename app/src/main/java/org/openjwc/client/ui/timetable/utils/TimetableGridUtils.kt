package org.openjwc.client.ui.timetable.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openjwc.client.data.models.Period
import java.time.Duration
import java.time.LocalTime

object TimetableGridUtils {
    /** 计算时间指示线的 Y 轴偏移量 */
    fun calculateTimeLineOffset(
        currentTime: LocalTime,
        periods: List<Period>,
        periodHeight: Dp
    ): Dp? {
        if (periods.isEmpty()) return null
        val first = periods.first()
        val last = periods.last()

        if (currentTime.isBefore(first.start) || currentTime.isAfter(last.end)) return null

        for (i in periods.indices) {
            val p = periods[i]
            if (!currentTime.isBefore(p.start) && !currentTime.isAfter(p.end)) {
                val total = Duration.between(p.start, p.end).toMinutes()
                val passed = Duration.between(p.start, currentTime).toMinutes()
                return (i + passed.toFloat() / total).dp * periodHeight.value
            } else if (i < periods.size - 1) {
                val nextP = periods[i + 1]
                if (currentTime.isAfter(p.end) && currentTime.isBefore(nextP.start)) {
                    return (i + 1).dp * periodHeight.value
                }
            }
        }
        return null
    }

    /** 将不连续的节次索引（如 1, 2, 4, 5）拆分为块（如 [1,2], [4,5]） */
    fun findContinuousBlocks(periods: List<Int>): List<List<Int>> {
        if (periods.isEmpty()) return emptyList()
        val blocks = mutableListOf<MutableList<Int>>()
        var currentBlock = mutableListOf(periods[0])
        for (i in 1 until periods.size) {
            if (periods[i] == periods[i - 1] + 1) {
                currentBlock.add(periods[i])
            } else {
                blocks.add(currentBlock)
                currentBlock = mutableListOf(periods[i])
            }
        }
        blocks.add(currentBlock)
        return blocks
    }

    /** 将周次集合转换为可读文本 */
    fun formatWeekRule(
        weekRule: Set<Int>,
        totalWeeks: Int,
        everyWeekStr: String,
        oddWeeksStr: String,
        evenWeeksStr: String,
        customWeeksStr: String
    ): String {
        if (weekRule.isEmpty()) return ""
        val sorted = weekRule.sorted()

        if (sorted.size >= totalWeeks && sorted.containsAll((1..totalWeeks).toList())) return everyWeekStr

        val oddWeeks = (1..totalWeeks).filter { it % 2 != 0 }
        val evenWeeks = (1..totalWeeks).filter { it % 2 == 0 }
        if (sorted == oddWeeks) return oddWeeksStr
        if (sorted == evenWeeks) return evenWeeksStr

        val ranges = mutableListOf<String>()
        if (sorted.isNotEmpty()) {
            var start = sorted[0]
            var end = sorted[0]
            for (i in 1 until sorted.size) {
                if (sorted[i] == end + 1) {
                    end = sorted[i]
                } else {
                    ranges.add(if (start == end) "$start" else "$start-$end")
                    start = sorted[i]
                    end = sorted[i]
                }
            }
            ranges.add(if (start == end) "$start" else "$start-$end")
        }
        return ranges.joinToString(", ") + " " + customWeeksStr
    }
}

/** 扩展函数，方便在 UI 中使用 */
fun Set<Int>.toDisplayText(
    totalWeeks: Int,
    everyWeekStr: String,
    oddWeeksStr: String,
    evenWeeksStr: String,
    customWeeksStr: String
): String = TimetableGridUtils.formatWeekRule(this, totalWeeks, everyWeekStr, oddWeeksStr, evenWeeksStr, customWeeksStr)
