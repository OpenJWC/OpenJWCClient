package org.openjwc.client.ui.timetable.view.grid

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openjwc.client.data.models.Course

/**
 * 课表格子长按拖动的共享状态。
 * 位置均为「网格内容坐标系」下的像素偏移（内容区左上角为原点，已包含左侧节次标签宽度）。
 */
@Stable
class TimetableDragState {
    var draggingCourse by mutableStateOf<Course?>(null)
        private set

    /** 被拖动块的左上角当前位置 */
    var dragPosition by mutableStateOf(Offset.Zero)
        private set

    /** 被拖动块的左上角原始位置（用于回弹） */
    var originalPosition by mutableStateOf(Offset.Zero)
        private set

    /** 原块尺寸，用于浮层出现时从原尺寸平滑过渡到目标尺寸 */
    var startWidth by mutableStateOf(0.dp)
        private set
    var startHeight by mutableStateOf(0.dp)
        private set

    val isDragging: Boolean get() = draggingCourse != null

    fun start(course: Course, blockTopLeft: Offset, width: Dp, height: Dp) {
        draggingCourse = course
        originalPosition = blockTopLeft
        dragPosition = blockTopLeft
        startWidth = width
        startHeight = height
    }

    fun drag(delta: Offset) {
        dragPosition += delta
    }

    fun moveTo(position: Offset) {
        dragPosition = position
    }

    fun reset() {
        draggingCourse = null
        dragPosition = Offset.Zero
        originalPosition = Offset.Zero
        startWidth = 0.dp
        startHeight = 0.dp
    }
}
