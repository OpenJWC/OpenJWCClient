package org.openjwc.client.ui.timetable.view.grid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.data.models.Course
import org.openjwc.client.ui.theme.rememberCourseColor
import java.time.DayOfWeek

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
fun CourseBlockPreview() {
    CourseBlock(
        modifier = Modifier,
        course = mockCourse,
        isCurrentWeek = false
    )
}

@Preview
@Composable
fun CourseBlockCurrentWeekPreview() {
    CourseBlock(
        modifier = Modifier,
        course = mockCourse,
        isCurrentWeek = true
    )
}

@Composable
fun CourseBlock(
    modifier: Modifier = Modifier,
    course: Course,
    isCurrentWeek: Boolean = true,
    isDragging: Boolean = false,
    initialScale: Float = 1f,
    scaleOverride: Float? = null,
    dragState: TimetableDragState? = null,
    onDragStart: ((Course) -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null,
    onClick: (Course) -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetScale = when {
        isDragging -> 1.06f
        isPressed -> 0.97f
        else -> 1f
    }
    // 用 Animatable 从 initialScale 起步，避免拖动浮层首次组合时直接跳到目标缩放值
    val scaleAnim = remember { Animatable(initialScale) }
    LaunchedEffect(targetScale) {
        scaleAnim.animateTo(
            targetValue = targetScale,
            animationSpec = spring(stiffness = 800f, dampingRatio = 0.5f)
        )
    }
    val scale = scaleOverride ?: scaleAnim.value

    val courseColor = rememberCourseColor(course.color)

    val containerColor = if (isCurrentWeek) {
        courseColor.container
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    }

    val contentColor = if (isCurrentWeek) {
        courseColor.content
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    // pointerInput 的 key 只有 course.id，课程被拖动后 id 不变、手势协程不会重启，
    // 因此必须用 rememberUpdatedState 读取最新的 course / 回调，否则会拿到移动前的旧对象
    val currentCourse by rememberUpdatedState(course)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    val dragModifier = if (dragState != null) {
        Modifier.pointerInput(course.id, dragState) {
            detectDragGesturesAfterLongPress(
                onDragStart = { currentOnDragStart?.invoke(currentCourse) },
                onDrag = { change, dragAmount ->
                    change.consume()
                    currentOnDrag?.invoke(dragAmount)
                },
                onDragEnd = { currentOnDragEnd?.invoke() },
                onDragCancel = { currentOnDragCancel?.invoke() }
            )
        }
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier
            .padding(2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .then(dragModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick(course) }
            )
    ) {
        val blockHeight = maxHeight
        val isShort = blockHeight < 80.dp
        Column(
            modifier = Modifier
                .padding(if (isShort) 4.dp else 8.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                text = course.name,
                color = contentColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isCurrentWeek) FontWeight.Bold else FontWeight.Normal,
                maxLines = if (isShort) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (!isShort) {
                if (course.teacher.isNotBlank()) {
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = course.teacher,
                        color = contentColor.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (course.location.isNotBlank()) {
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = "@" + course.location,
                        color = contentColor.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
