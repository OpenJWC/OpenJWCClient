package org.openjwc.client.ui.timetable.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.openjwc.client.R

@Composable
fun Set<Int>.toDisplayText(totalWeeks: Int = 16): String {
    if (isEmpty()) return ""
    val sorted = this.toList().sorted()

    val everyWeek = (1..totalWeeks).toSet()
    val oddWeeks = (1..totalWeeks).filter { it % 2 != 0 }.toSet()
    val evenWeeks = (1..totalWeeks).filter { it % 2 == 0 }.toSet()

    return when {
        this == everyWeek -> stringResource(R.string.every_week)
        this == oddWeeks -> stringResource(R.string.odd_weeks)
        this == evenWeeks -> stringResource(R.string.even_weeks)
        else -> {
            val rangeText = formatRangeText(this)
            "$rangeText 周" // TODO: I18n required
        }
    }
}

/**
 * 💡 核心算法：合并连续周
 * 例如 [1, 2, 3, 5, 8, 9] -> "1-3, 5, 8-9"
 */
private fun formatRangeText(weeks: Set<Int>): String {
    if (weeks.isEmpty()) return ""
    val sorted = weeks.sorted()
    val segments = mutableListOf<String>()

    var i = 0
    while (i < sorted.size) {
        val start = sorted[i]
        var end = start
        while (i + 1 < sorted.size && sorted[i + 1] == end + 1) {
            end = sorted[++i]
        }
        segments.add(if (start == end) "$start" else "$start-$end")
        i++
    }
    return segments.joinToString(", ")
}
