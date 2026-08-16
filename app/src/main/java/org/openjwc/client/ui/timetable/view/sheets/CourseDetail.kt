package org.openjwc.client.ui.timetable.view.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openjwc.client.R
import org.openjwc.client.data.models.Course
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.timetable.edit.components.CardItem
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = dragHandle,
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
    ) {
        Column {
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
    color = Color(0xFF415F91),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Heading(
            text = course.name,
            isCurrentWeek = course.weekRule.contains(currentWeek)
        )

        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoSection(course, totalWeeks)
                }
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoSection(course, totalWeeks)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.padding(horizontal = 16.dp)) {
                    ActionButtons(
                        horizontally = true,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
fun Heading(
    text: String,
    isCurrentWeek: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
    ) {
        if (!isCurrentWeek) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.not_in_current_week),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun InfoSection(
    course: Course,
    totalWeeks: Int
) {
    val weekDescription = course.weekRule.toDisplayText(
        totalWeeks = totalWeeks,
        everyWeekStr = stringResource(R.string.every_week),
        oddWeeksStr = stringResource(R.string.odd_weeks),
        evenWeeksStr = stringResource(R.string.even_weeks),
        customWeeksStr = stringResource(R.string.week_number)
    )

    val locale = LocalLocale.current.platformLocale
    val periodDescription = stringResource(
        R.string.custom_periods_format,
        course.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
        course.startPeriod,
        course.startPeriod + course.duration - 1
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SegmentedColumn {
            item {
                SettingsBaseWidget(
                    icon = Icons.Default.Place,
                    iconColor = course.color,
                    title = course.location.ifBlank { stringResource(R.string.location_not_specified) },
                    description = course.teacher.ifBlank { stringResource(R.string.teacher_not_specified) }
                )
            }
            item {
                SettingsBaseWidget(
                    icon = Icons.Default.CalendarMonth,
                    title = weekDescription,
                    description = stringResource(R.string.week_number)
                )
            }
            item {
                SettingsBaseWidget(
                    icon = Icons.Default.Schedule,
                    title = periodDescription,
                    description = stringResource(R.string.period_number)
                )
            }
        }

        // 备注：独立分组，与上方信息组同宽
        if (course.note.isNotBlank()) {
            SegmentedColumn {
                item {
                    CardItem {
                        Text(
                            text = course.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                    .height(52.dp),
                onClick = onEdit
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.edit_course), fontWeight = FontWeight.Bold)
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = onEdit
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.edit_course), fontWeight = FontWeight.Bold)
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
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
    evenWeeksStr: String,
    customWeeksStr: String = ""
): String {
    if (isEmpty()) return ""
    val sorted = this.toList().sorted()

    if (sorted.size >= totalWeeks && sorted.containsAll((1..totalWeeks).toList())) {
        return everyWeekStr
    }

    val oddWeeks = (1..totalWeeks).filter { it % 2 != 0 }
    val evenWeeks = (1..totalWeeks).filter { it % 2 == 0 }

    if (sorted == oddWeeks) return oddWeeksStr
    if (sorted == evenWeeks) return evenWeeksStr

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
    return ranges.joinToString(", ") + " " + customWeeksStr
}
