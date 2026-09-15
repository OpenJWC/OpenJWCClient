package org.openjwc.client.ui.timetable.view.grid

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import org.openjwc.client.data.models.Course
import org.openjwc.client.ui.timetable.utils.TimetableGridUtils
import java.time.DayOfWeek

/** 非本周课程的一个可见分段及其并排泳道信息 */
private data class LaneBlock(
    val course: Course,
    val periods: List<Int>,
    val lane: Int,
    val laneCount: Int
)

@Composable
fun CourseColumnScope(
    modifier: Modifier = Modifier,
    day: DayOfWeek,
    courses: List<Course>,
    currentWeek: Int,
    showNonCurrentWeek: Boolean,
    periodHeight: Dp,
    totalPeriods: Int,
    onCourseClick: (Course) -> Unit,
    dragState: TimetableDragState? = null,
    onCourseDragStart: ((Course) -> Unit)? = null,
    onCourseDrag: ((Offset) -> Unit)? = null,
    onCourseDragEnd: (() -> Unit)? = null,
    onCourseDragCancel: (() -> Unit)? = null
) {
    // 💡 修正 1：必须先根据 day 过滤出当天的课
    val dayCourses = remember(courses, day) {
        courses.filter { it.dayOfWeek == day }
    }

    // 预计算本列的派生布局数据（本周/非本周课程、占用节次、非本周块切分 + 泳道）
    val draggingId = dragState?.draggingCourse?.id
    val layout = remember(dayCourses, currentWeek, showNonCurrentWeek, draggingId) {
        val (thisWeek, otherWeeks) = dayCourses.partition { it.weekRule.contains(currentWeek) }

        // 本周课程占用的节次；拖动中的课程不再占位，露出其下方的非本周课程
        val occupiedByThisWeek = thisWeek
            .filter { it.id != draggingId }
            .flatMap { c -> c.startPeriod until (c.startPeriod + c.duration) }
            .toSet()

        // 非本周课程只按【本周课程】裁剪，彼此之间改用并排泳道而非互相遮盖
        val otherSegments = mutableListOf<Pair<Course, List<Int>>>()
        if (showNonCurrentWeek) {
            otherWeeks.forEach { course ->
                val courseRange = (course.startPeriod until (course.startPeriod + course.duration))
                val visiblePeriods = courseRange.filter { it !in occupiedByThisWeek }
                if (visiblePeriods.isNotEmpty()) {
                    TimetableGridUtils.findContinuousBlocks(visiblePeriods).forEach { block ->
                        otherSegments.add(course to block)
                    }
                }
            }
        }

        thisWeek to assignLanes(otherSegments)
    }
    val (thisWeek, otherBlocks) = layout

    BoxWithConstraints(modifier = modifier.height(periodHeight * totalPeriods)) {
        val columnWidth = maxWidth

        // 2. 绘制非本周课程（背景层，重叠时并排）
        otherBlocks.forEach { block ->
            val laneCount = block.laneCount.coerceAtLeast(1)
            val laneWidth = columnWidth / laneCount
            val isBeingDragged = dragState?.draggingCourse?.id == block.course.id
            CourseBlock(
                course = block.course,
                isCurrentWeek = false,
                dragState = dragState,
                onDragStart = onCourseDragStart,
                onDrag = onCourseDrag,
                onDragEnd = onCourseDragEnd,
                onDragCancel = onCourseDragCancel,
                modifier = Modifier
                    .width(laneWidth)
                    .offset(
                        x = laneWidth * block.lane,
                        y = periodHeight * (block.periods.first() - 1)
                    )
                    .height(periodHeight * block.periods.size)
                    .alpha(if (isBeingDragged) 0f else 1f)
                    .zIndex(1f),
                onClick = onCourseClick
            )
        }

        // 3. 绘制本周课程（顶层，遮盖一切）
        thisWeek.forEach { course ->
            val isBeingDragged = dragState?.draggingCourse?.id == course.id
            CourseBlock(
                course = course,
                isCurrentWeek = true,
                dragState = dragState,
                onDragStart = onCourseDragStart,
                onDrag = onCourseDrag,
                onDragEnd = onCourseDragEnd,
                onDragCancel = onCourseDragCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = periodHeight * (course.startPeriod - 1))
                    .height(periodHeight * course.duration)
                    .alpha(if (isBeingDragged) 0f else 1f)
                    .zIndex(if (isBeingDragged) 0f else 2f),
                onClick = onCourseClick
            )
        }
    }
}

/**
 * 为互相重叠的非本周课程分段分配并排泳道：
 * 先把传递性重叠的分段归为一组，组内贪心选择第一条空闲泳道。
 */
private fun assignLanes(segments: List<Pair<Course, List<Int>>>): List<LaneBlock> {
    if (segments.isEmpty()) return emptyList()
    val sorted = segments.sortedBy { it.second.firstOrNull() ?: 0 }
    val result = mutableListOf<LaneBlock>()
    val group = mutableListOf<Pair<Course, List<Int>>>()
    var groupEnd = -1

    fun flushGroup() {
        if (group.isEmpty()) return
        val laneEnds = mutableListOf<Int>()
        val laneOf = IntArray(group.size)
        group.forEachIndexed { index, (_, periods) ->
            val start = periods.first()
            val end = periods.last()
            val freeLane = laneEnds.indexOfFirst { it < start }
            if (freeLane == -1) {
                laneEnds.add(end)
                laneOf[index] = laneEnds.size - 1
            } else {
                laneEnds[freeLane] = end
                laneOf[index] = freeLane
            }
        }
        val laneCount = laneEnds.size
        group.forEachIndexed { index, (course, periods) ->
            result.add(LaneBlock(course, periods, laneOf[index], laneCount))
        }
        group.clear()
        groupEnd = -1
    }

    for (segment in sorted) {
        val start = segment.second.firstOrNull() ?: continue
        val end = segment.second.lastOrNull() ?: continue
        if (group.isNotEmpty() && start > groupEnd) flushGroup()
        group.add(segment)
        groupEnd = maxOf(groupEnd, end)
    }
    flushGroup()
    return result
}
