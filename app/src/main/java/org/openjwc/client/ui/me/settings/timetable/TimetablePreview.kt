package org.openjwc.client.ui.me.settings.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.Period
import org.openjwc.client.data.models.SemesterConfig
import org.openjwc.client.ui.timetable.view.components.TimetableHeader
import org.openjwc.client.ui.timetable.view.grid.CourseColumnScope
import org.openjwc.client.ui.timetable.view.grid.GridBackgroundLayer
import org.openjwc.client.ui.timetable.view.grid.TimeIndicatorLine
import org.openjwc.client.viewmodels.TimetableDisplayPrefs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

private const val PREVIEW_WEEK = 1

/**
 * 课程表设置页的静态预览：复用真实的表头、网格背景与课程卡片，
 * 仅用少量示例课程展示各项显示开关的效果。
 */
@Composable
fun TimetablePreview(
    prefs: TimetableDisplayPrefs,
    modifier: Modifier = Modifier
) {
    val locale = LocalLocale.current.platformLocale

    val config = remember {
        SemesterConfig(
            startDate = LocalDate.of(2026, 9, 14),
            weeks = 16,
            visibleDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ),
            periods = listOf(
                Period(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
                Period(2, LocalTime.of(8, 50), LocalTime.of(9, 35)),
                Period(3, LocalTime.of(9, 50), LocalTime.of(10, 35)),
                Period(4, LocalTime.of(10, 40), LocalTime.of(11, 25))
            )
        )
    }
    val sortedVisibleDays = remember(config.visibleDays) { config.visibleDays.sortedBy { it.value } }

    val courses = remember {
        listOf(
            previewCourse(1, "高等数学", "张老师", "N2-304", DayOfWeek.MONDAY, 1, 2, 0xFF1565C0, setOf(1)),
            previewCourse(2, "大学英语", "李老师", "J1-101", DayOfWeek.WEDNESDAY, 2, 2, 0xFF2E7D32, setOf(1)),
            previewCourse(3, "大学物理", "王老师", "N2-212", DayOfWeek.FRIDAY, 3, 2, 0xFF6A1B9A, setOf(1)),
            // 非本周示例（仅在第 2 周）
            previewCourse(4, "体育", "赵老师", "操场", DayOfWeek.TUESDAY, 3, 2, 0xFFEF6C00, setOf(2))
        )
    }

    val periodHeight = 56.dp
    val timeLabelWidth = 44.dp
    val titleHeight = 44.dp

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        TimetableHeader(
            currentWeek = PREVIEW_WEEK,
            startDate = config.startDate,
            sortedVisibleDays = sortedVisibleDays,
            timeLabelWidth = timeLabelWidth,
            titleHeight = titleHeight,
            locale = locale,
            showDate = prefs.showDate
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            GridBackgroundLayer(
                config = config,
                sortedVisibleDays = sortedVisibleDays,
                periodHeight = periodHeight,
                timeLabelWidth = timeLabelWidth,
                activePeriodIndex = -1,
                showPeriodTime = prefs.showPeriodTime,
                onEmptySlotClick = { _, _ -> }
            )

            if (prefs.showTimeline) {
                TimeIndicatorLine(
                    periods = config.periods,
                    periodHeight = periodHeight,
                    timeLabelWidth = timeLabelWidth,
                    fixedTime = LocalTime.of(9, 55)
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(start = timeLabelWidth)) {
                sortedVisibleDays.forEach { day ->
                    CourseColumnScope(
                        modifier = Modifier.weight(1f),
                        day = day,
                        courses = courses,
                        currentWeek = PREVIEW_WEEK,
                        showNonCurrentWeek = prefs.showNonCurrentWeek,
                        periodHeight = periodHeight,
                        totalPeriods = config.periods.size,
                        onCourseClick = {}
                    )
                }
            }
        }
    }
}

private fun previewCourse(
    id: Long,
    name: String,
    teacher: String,
    location: String,
    day: DayOfWeek,
    startPeriod: Int,
    duration: Int,
    colorArgb: Long,
    weeks: Set<Int>
) = Course(
    id = id,
    tableId = 0,
    name = name,
    teacher = teacher,
    location = location,
    dayOfWeek = day,
    startPeriod = startPeriod,
    duration = duration,
    color = Color(colorArgb.toInt()),
    weekRule = weeks,
    note = ""
)
