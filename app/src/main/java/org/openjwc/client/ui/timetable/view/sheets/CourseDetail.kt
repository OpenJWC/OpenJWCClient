package org.openjwc.client.ui.timetable.view.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openjwc.client.R
import org.openjwc.client.data.models.Course
import java.time.DayOfWeek
import java.time.format.TextStyle


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailSheet(
    windowSizeClass: WindowSizeClass,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    dragHandle: @Composable () -> Unit,
    course: Course,
    currentWeek: Int,
    totalWeeks: Int = 16,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isWideScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = dragHandle,
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.verticalScroll(scrollState)
        ) {
            CourseDetailContent(
                course = course,
                isWideScreen = isWideScreen,
                onEdit = onEdit,
                onDelete = onDelete,
                currentWeek = currentWeek,
                totalWeeks = totalWeeks
            )
        }
    }
}

private val mockCourse = Course(
    id = 0,
    tableId = 0,
    name = "微积分 I",
    teacher = "张老师",
    location = "教一 101",
    dayOfWeek = DayOfWeek.MONDAY,
    startPeriod = 1,
    duration = 2,
    color = Color.Red,
    weekRule = (1..16 step 2).toSet(),
    note = "带好计算器"
)

@Preview
@Composable
fun CourseDetailContentPreview() {
    CourseDetailContent(
        course = mockCourse,
        currentWeek = 1,
        totalWeeks = 16,
        onEdit = {},
        onDelete = {}
    )
}


@Composable
fun CourseDetailContent(
    course: Course,
    isWideScreen: Boolean = true,
    currentWeek: Int,
    totalWeeks: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.background(
            MaterialTheme.colorScheme.surface
        )
    ) {
        Heading(
            text = course.name,
            isCurrentWeek = course.weekRule.contains(currentWeek)
        )

        if (isWideScreen) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoSection(course, totalWeeks)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ActionButtons(
                        horizontally = false,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                InfoSection(course, totalWeeks)
                Spacer(Modifier.height(24.dp))
                ActionButtons(
                    horizontally = true,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
fun Heading(
    text: String,
    isCurrentWeek: Boolean
) {
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        if (!isCurrentWeek) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.error,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.not_in_current_week),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Composable
fun InfoSection(
    course: Course,
    totalWeeks: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

        if (course.location.isNotBlank() || course.teacher.isNotBlank()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconInfoRow(
                    icon = Icons.Default.Place,
                    title = course.location.ifBlank { stringResource(R.string.location_not_specified) },
                    subtitle = course.teacher.ifBlank { stringResource(R.string.teacher_not_specified) },
                    color = course.color
                )
            }
        }

        if (course.note.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = course.note,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        val weekDescription = course.weekRule.toDisplayText(
            totalWeeks = totalWeeks,
            everyWeekStr = stringResource(R.string.every_week),
            oddWeeksStr = stringResource(R.string.odd_weeks),
            evenWeeksStr = stringResource(R.string.even_weeks)
        )

        val locale = LocalLocale.current.platformLocale
        val periodDescription = stringResource(
            R.string.custom_periods_format,
            course.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
            course.startPeriod,
            course.startPeriod + course.duration - 1
        )

        val isWeekTooLong = weekDescription.length > 18
        val isPeriodTooLong = periodDescription.length > 18

        if (isWeekTooLong || isPeriodTooLong) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailItem(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.week_number),
                    value = weekDescription
                )
                DetailItem(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.period_number),
                    value = periodDescription
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.week_number),
                    value = weekDescription
                )
                DetailItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.period_number),
                    value = periodDescription
                )
            }
        }
    }
}

@Composable
fun DetailItem(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun IconInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp),
                tint = color
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionButtons(
    horizontally: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    if (horizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp), onClick = onEdit
            ) {
                Text(stringResource(R.string.edit_course), fontWeight = FontWeight.Bold)
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp), onClick = onEdit
            ) {
                Text(stringResource(R.string.edit_course), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.padding(6.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

/**
 * 格式化周次集合为可读文本
 */
private fun Set<Int>.toDisplayText(
    totalWeeks: Int,
    everyWeekStr: String,
    oddWeeksStr: String,
    evenWeeksStr: String
): String {
    if (isEmpty()) return ""
    val sorted = this.toList().sorted()

    // 判断是否为每周
    if (sorted.size >= totalWeeks && sorted.containsAll((1..totalWeeks).toList())) {
        return everyWeekStr
    }

    // 判断单双周
    val oddWeeks = (1..totalWeeks).filter { it % 2 != 0 }
    val evenWeeks = (1..totalWeeks).filter { it % 2 == 0 }

    if (sorted == oddWeeks) return oddWeeksStr
    if (sorted == evenWeeks) return evenWeeksStr

    // 格式化范围，如 "1-3, 5, 7-10 周"
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
    return ranges.joinToString(", ") + " 周"
}
