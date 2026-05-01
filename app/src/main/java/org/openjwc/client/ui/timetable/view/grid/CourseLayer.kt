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

    Box(modifier = modifier.height(periodHeight * totalPeriods)) {
        // 1. 分离本周和非本周
        val (thisWeek, otherWeeks) = remember(dayCourses, currentWeek) {
            dayCourses.partition { it.weekRule.contains(currentWeek) }
        }

        // 💡 修正 2：建立一个已占用节次的集合，用于“防重叠”
        // 初始占用者是【本周课程】
        // 注意：这里不要使用 remember，因为我们在下面循环中会通过 addAll 修改它。
        // 如果使用 remember，当 courses 变化但 thisWeek 内容不变时，会拿到上一次重组污染后的 Set 实例。
        val occupiedPeriods = thisWeek.flatMap { c -> c.startPeriod until (c.startPeriod + c.duration) }
            .toMutableSet()

        // 2. 绘制非本周课程（背景层）
        if (showNonCurrentWeek) {
            // 排序：优先显示长课（大课），或者你可以根据需求调整排序
            val sortedOtherWeeks = remember(otherWeeks) {
                otherWeeks.sortedByDescending { it.duration }
            }

            sortedOtherWeeks.forEach { course ->
                val courseRange = (course.startPeriod until (course.startPeriod + course.duration))

                // 找出当前非本周课程中，哪些节次没被占（裁剪逻辑）
                val visiblePeriods = courseRange.filter { it !in occupiedPeriods }

                if (visiblePeriods.isNotEmpty()) {
                    TimetableGridUtils.findContinuousBlocks(visiblePeriods).forEach { block ->
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
                    // 💡 修正 3：绘制完一段非本周课，也要更新占位，防止其他非本周课盖上来
                    occupiedPeriods.addAll(visiblePeriods)
                }
            }
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
