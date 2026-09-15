package org.openjwc.client.ui.timetable.view.grid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.Period
import org.openjwc.client.data.models.SemesterConfig
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.ui.timetable.view.components.TimetableHeader
import java.time.DayOfWeek
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale
import kotlin.math.roundToInt

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
        onEmptySlotClick = { _, _ -> },
        onCourseMove = { _, _, _ -> }
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
    onCourseMove: (Course, DayOfWeek, Int) -> Unit,
    activePeriodIndex: Int = -1,
    minPeriodHeight: Dp = 60.dp,
    timeLabelWidth: Dp = 44.dp,
    titleHeight: Dp = 48.dp
) {
    val config = tableMetadata.semesterConfig
    val locale = LocalLocale.current.platformLocale

    val sortedVisibleDays = remember(config.visibleDays) {
        config.visibleDays.sortedBy { it.value }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        val gridHeight = if (availableHeight.isFinite) availableHeight - titleHeight else 600.dp
        val periodHeight = maxOf(minPeriodHeight, gridHeight / config.periods.size)

        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val dragState = remember { TimetableDragState() }
        var settleScale by remember { mutableStateOf<Float?>(null) }
        var pendingMove by remember { mutableStateOf<Triple<Course, DayOfWeek, Int>?>(null) }
        // 刚落位课程的浮层尺寸，用于让新块从该尺寸过渡到目标尺寸
        var lastDropped by remember { mutableStateOf<Pair<Long, DpSize>?>(null) }

        val timeLabelWidthPx = with(density) { timeLabelWidth.toPx() }
        val periodHeightPx = with(density) { periodHeight.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val dayCount = sortedVisibleDays.size
        val colWidthPx = if (dayCount > 0) (maxWidthPx - timeLabelWidthPx) / dayCount else 0f
        val colWidthDp = with(density) { colWidthPx.toDp() }

        fun courseTopLeft(course: Course): Offset {
            val dayIndex = sortedVisibleDays.indexOf(course.dayOfWeek).coerceAtLeast(0)
            val periodIndex = config.periods.indexOfFirst { it.index == course.startPeriod }.coerceAtLeast(0)
            return Offset(
                x = timeLabelWidthPx + dayIndex * colWidthPx,
                y = periodHeightPx * periodIndex
            )
        }

        fun snapBack() {
            val start = dragState.dragPosition
            val end = dragState.originalPosition
            scope.launch {
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(durationMillis = 220)) {
                    dragState.moveTo(lerp(start, end, value))
                    settleScale = 1.06f - 0.06f * value
                }
                dragState.reset()
                settleScale = null
            }
        }

        val onCourseDragStart: (Course, Dp, Dp) -> Unit = { course, width, height ->
            lastDropped = null
            dragState.start(course, courseTopLeft(course), width, height)
        }
        val onCourseDrag: (Offset) -> Unit = { delta -> dragState.drag(delta) }
        val onCourseDragEnd: () -> Unit = {
            val course = dragState.draggingCourse
            if (course == null) {
                dragState.reset()
            } else {
                val target = resolveDropTarget(
                    course = course,
                    dragPosition = dragState.dragPosition,
                    colWidthPx = colWidthPx,
                    timeLabelWidthPx = timeLabelWidthPx,
                    periodHeightPx = periodHeightPx,
                    sortedVisibleDays = sortedVisibleDays,
                    periods = config.periods,
                    courses = courses
                )
                if (target != null &&
                    (target.first != course.dayOfWeek || target.second != course.startPeriod)
                ) {
                    // 先让浮层平滑落位（位移 + 缩放回落），再提交数据变更
                    val targetTopLeft = courseTopLeft(
                        course.copy(dayOfWeek = target.first, startPeriod = target.second)
                    )
                    val start = dragState.dragPosition
                    scope.launch {
                        val anim = Animatable(0f)
                        anim.animateTo(1f, tween(durationMillis = 220)) {
                            dragState.moveTo(lerp(start, targetTopLeft, value))
                            settleScale = 1.06f - 0.06f * value
                        }
                        // 提交数据变更，但保留浮层，等数据真正反映到列表后再移除
                        onCourseMove(course, target.first, target.second)
                        lastDropped = course.id to DpSize(colWidthDp, periodHeight * course.duration)
                        pendingMove = Triple(course, target.first, target.second)
                    }
                } else {
                    snapBack()
                }
            }
        }
        val onCourseDragCancel: () -> Unit = { snapBack() }

        // 数据反映落位结果后再移除浮层，避免异步写入期间旧位置闪现
        LaunchedEffect(courses, pendingMove) {
            val pending = pendingMove ?: return@LaunchedEffect
            val (movedCourse, day, period) = pending
            val reflected = courses.any {
                it.id == movedCourse.id && it.dayOfWeek == day && it.startPeriod == period
            }
            if (reflected) {
                dragState.reset()
                settleScale = null
                pendingMove = null
            }
        }

        // 兜底：写入失败或长时间未反映时，最多等 1s 后强制移除浮层
        LaunchedEffect(pendingMove) {
            if (pendingMove != null) {
                delay(1000)
                if (pendingMove != null) {
                    dragState.reset()
                    settleScale = null
                    pendingMove = null
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            TimetableHeader(
                currentWeek = currentWeek,
                startDate = config.startDate,
                sortedVisibleDays = sortedVisibleDays,
                timeLabelWidth = timeLabelWidth,
                titleHeight = titleHeight,
                locale = locale,
                showDate = showDate
            )

            Box(modifier = Modifier.fillMaxWidth()) {
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
                    TimeIndicatorLine(
                        periods = config.periods,
                        periodHeight = periodHeight,
                        timeLabelWidth = timeLabelWidth
                    )
                }

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
                            onCourseClick = onCourseClick,
                            dragState = dragState,
                            onCourseDragStart = onCourseDragStart,
                            onCourseDrag = onCourseDrag,
                            onCourseDragEnd = onCourseDragEnd,
                            onCourseDragCancel = onCourseDragCancel,
                            lastDropped = lastDropped
                        )
                    }
                }

                // 拖动中的浮层：渲染在网格顶层，跟随手指移动
                dragState.draggingCourse?.let { course ->
                    // 浮层从原块尺寸平滑过渡到整列整课尺寸（避免非本周窄块/分段块突然变形）
                    val overlayWidth = remember {
                        Animatable(dragState.startWidth, Dp.VectorConverter)
                    }
                    val overlayHeight = remember {
                        Animatable(dragState.startHeight, Dp.VectorConverter)
                    }
                    LaunchedEffect(course.id, colWidthDp, periodHeight) {
                        launch {
                            overlayWidth.animateTo(
                                colWidthDp,
                                spring(stiffness = 700f, dampingRatio = 0.85f)
                            )
                        }
                        launch {
                            overlayHeight.animateTo(
                                periodHeight * course.duration,
                                spring(stiffness = 700f, dampingRatio = 0.85f)
                            )
                        }
                    }
                    CourseBlock(
                        course = course,
                        isCurrentWeek = course.weekRule.contains(currentWeek),
                        isDragging = true,
                        initialScale = 0.97f,
                        scaleOverride = settleScale,
                        modifier = Modifier
                            .width(overlayWidth.value)
                            .height(overlayHeight.value)
                            .offset {
                                IntOffset(
                                    dragState.dragPosition.x.roundToInt(),
                                    dragState.dragPosition.y.roundToInt()
                                )
                            }
                            .zIndex(10f),
                        onClick = {}
                    )
                }
            }
        }
    }
}

/**
 * 根据被拖动块左上角的位置计算落点（星期 + 起始节次）。
 * 若越界或与其它课程冲突则返回 null（调用方据此回弹）。
 */
private fun resolveDropTarget(
    course: Course,
    dragPosition: Offset,
    colWidthPx: Float,
    timeLabelWidthPx: Float,
    periodHeightPx: Float,
    sortedVisibleDays: List<DayOfWeek>,
    periods: List<Period>,
    courses: List<Course>
): Pair<DayOfWeek, Int>? {
    if (colWidthPx <= 0f || periodHeightPx <= 0f) return null
    if (periods.isEmpty() || sortedVisibleDays.isEmpty()) return null

    // 以块的水平中心决定目标列
    val centerX = dragPosition.x + colWidthPx / 2f
    val dayIndex = ((centerX - timeLabelWidthPx) / colWidthPx)
        .toInt()
        .coerceIn(0, sortedVisibleDays.size - 1)

    // 以块的上边缘决定起始节次，并保证整块落在网格内
    val maxStartIndex = (periods.size - course.duration).coerceAtLeast(0)
    val periodIndex = (dragPosition.y / periodHeightPx)
        .roundToInt()
        .coerceIn(0, maxStartIndex)

    val day = sortedVisibleDays[dayIndex]
    val startPeriod = periods[periodIndex].index
    val endPeriod = startPeriod + course.duration - 1
    if (endPeriod > periods.last().index) return null

    val candidate = course.copy(dayOfWeek = day, startPeriod = startPeriod)
    val hasConflict = courses.any { other ->
        other.id != course.id && candidate isConflictingWith other
    }
    if (hasConflict) return null

    return day to startPeriod
}
