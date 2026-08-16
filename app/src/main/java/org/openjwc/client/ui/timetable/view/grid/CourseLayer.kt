package org.openjwc.client.ui.timetable.view.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import org.openjwc.client.data.models.Course
import org.openjwc.client.ui.timetable.utils.TimetableGridUtils
import java.time.DayOfWeek

@Composable
fun CourseColumnScope(
    modifier: Modifier = Modifier,
    day: DayOfWeek,
    courses: List<Course>,
    currentWeek: Int,
    showNonCurrentWeek: Boolean,
    periodHeight: Dp,
    totalPeriods: Int,
    onCourseClick: (Course) -> Unit
) {
    // 💡 修正 1：必须先根据 day 过滤出当天的课
    val dayCourses = remember(courses, day) {
        courses.filter { it.dayOfWeek == day }
    }

    // 预计算本列的派生布局数据（本周/非本周课程、占用节次、非本周块切分），
    // 避免每次重组都重新 flatMap + findContinuousBlocks
    val layout = remember(dayCourses, currentWeek, showNonCurrentWeek) {
        val (thisWeek, otherWeeks) = dayCourses.partition { it.weekRule.contains(currentWeek) }

        // 初始占用者是【本周课程】
        val occupiedPeriods = thisWeek.flatMap { c -> c.startPeriod until (c.startPeriod + c.duration) }
            .toMutableSet()

        // 绘制非本周课程（背景层），排序：优先显示长课
        val otherBlocks = mutableListOf<Pair<Course, List<Int>>>()
        if (showNonCurrentWeek) {
            otherWeeks.sortedByDescending { it.duration }.forEach { course ->
                val courseRange = (course.startPeriod until (course.startPeriod + course.duration))

                // 找出当前非本周课程中，哪些节次没被占（裁剪逻辑）
                val visiblePeriods = courseRange.filter { it !in occupiedPeriods }

                if (visiblePeriods.isNotEmpty()) {
                    TimetableGridUtils.findContinuousBlocks(visiblePeriods).forEach { block ->
                        otherBlocks.add(course to block)
                    }
                    // 绘制完一段非本周课，也要更新占位，防止其他非本周课盖上来
                    occupiedPeriods.addAll(visiblePeriods)
                }
            }
        }

        Triple(thisWeek, otherBlocks, occupiedPeriods)
    }
    val (thisWeek, otherBlocks, _) = layout

    Box(modifier = modifier.height(periodHeight * totalPeriods)) {
        // 2. 绘制非本周课程（背景层）
        otherBlocks.forEach { (course, block) ->
            CourseBlock(
                course = course,
                isCurrentWeek = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = periodHeight * (block.first() - 1))
                    .height(periodHeight * block.size)
                    .zIndex(1f),
                onClick = onCourseClick
            )
        }

        // 3. 绘制本周课程（顶层，遮盖一切）
        thisWeek.forEach { course ->
            CourseBlock(
                course = course,
                isCurrentWeek = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = periodHeight * (course.startPeriod - 1))
                    .height(periodHeight * course.duration)
                    .zIndex(2f),
                onClick = onCourseClick
            )
        }
    }
}
