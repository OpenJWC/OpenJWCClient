package org.openjwc.client.ui.timetable.view.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.SemesterConfig
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.ui.timetable.view.components.TimetableHeader
import java.time.DayOfWeek
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

private val mockCourse = Course(
    id = 0,
    tableId = 0,
    name = "测试课程",
    teacher = "老师",
    location = "地点",
    dayOfWeek = DayOfWeek.MONDAY,
    startPeriod = 1,
    duration = 2,
    color = Color.Red,
    weekRule = (1..16).toSet(),
    note = "备注"
)

@Preview
@Composable
fun TimetableGridPreview() {
    TimetableGrid(
        tableMetadata = TableMetadata(
            id = 0L,
            tableName = "测试课表",
            semesterConfig = SemesterConfig.default(),
            isCurrent = true
        ),
        currentWeek = 2,
        courses = listOf(mockCourse),
        onCourseClick = {},
        onEmptySlotClick = { _, _ -> }
    )

}

@Composable
fun TimetableGrid(
    tableMetadata: TableMetadata,
    courses: List<Course>,
    currentWeek: Int,
    showNonCurrentWeek: Boolean = false,
    showTimeLine: Boolean = true,
    showDate: Boolean = true,
    showPeriodTime: Boolean = true,
    onCourseClick: (Course) -> Unit,
    onEmptySlotClick: (DayOfWeek, Int) -> Unit,
    activePeriodIndex: Int = -1,
    minPeriodHeight: Dp = 60.dp,
    timeLabelWidth: Dp = 44.dp,
    titleHeight: Dp = 48.dp
) {
    val config = tableMetadata.semesterConfig
    val locale = LocalLocale.current.platformLocale

    // 提取排序后的可见天数
    val sortedVisibleDays = remember(config.visibleDays) {
        config.visibleDays.sortedBy { it.value }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        // 注意：如果外层有 Scroll，maxHeight 可能是 Infinity，这里处理一下
        val gridHeight = if (availableHeight.isFinite) availableHeight - titleHeight else 600.dp
        val periodHeight = maxOf(minPeriodHeight, gridHeight / config.periods.size)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // --- 💡 星期标题栏 ---
            TimetableHeader(
                currentWeek = currentWeek,
                startDate = config.startDate,
                sortedVisibleDays = sortedVisibleDays,
                timeLabelWidth = timeLabelWidth,
                titleHeight = titleHeight,
                locale = locale,
                showDate = showDate
            )

            // --- 核心网格区 ---
            Box(modifier = Modifier.fillMaxWidth()) {
                // A. 底层网格
                GridBackgroundLayer(
                    config = config,
                    sortedVisibleDays = sortedVisibleDays,
                    periodHeight = periodHeight,
                    timeLabelWidth = timeLabelWidth,
                    activePeriodIndex = activePeriodIndex,
                    showPeriodTime = showPeriodTime,
                    onEmptySlotClick = onEmptySlotClick
                )

                if (showTimeLine) {
                    // B. 中层时间指示线
                    TimeIndicatorLine(
                        periods = config.periods,
                        periodHeight = periodHeight,
                        timeLabelWidth = timeLabelWidth
                    )
                }

                // C. 顶层课程卡片
                Row(modifier = Modifier.fillMaxWidth().padding(start = timeLabelWidth)) {
                    sortedVisibleDays.forEach { day ->
                        CourseColumnScope(
                            modifier = Modifier.weight(1f),
                            day = day,
                            courses = courses,
                            currentWeek = currentWeek,
                            showNonCurrentWeek = showNonCurrentWeek,
                            periodHeight = periodHeight,
                            totalPeriods = config.periods.size,
                            onCourseClick = onCourseClick
                        )
                    }
                }
            }
        }
    }
}
