package org.openjwc.client.ui.timetable.edit.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.data.models.Course
import org.openjwc.client.ui.timetable.edit.components.ErrorBox
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Preview
@Composable
fun DayOfWeekSelectorPreview() {
    DayOfWeekSelector(selectedDay = DayOfWeek.MONDAY, onDaySelected = {})
}


/**
 * 💡 锁定高度与状态的选择器，防止重组抖动
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayOfWeekSelector(
    selectedDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit
) {
    val locale = remember { Locale.getDefault() }
    val dayNames = remember(locale) {
        DayOfWeek.entries.map { it to it.getDisplayName(TextStyle.SHORT, locale) }
    }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp), // 物理锁定高度
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dayNames.forEach { (day, name) ->
            key(day) { // 锁定节点
                FilterChip(
                    selected = selectedDay == day,
                    onClick = { onDaySelected(day) },
                    label = { Text(text = name) }
                )
            }
        }
    }
}

@Preview
@Composable
fun CourseTimeSectionPreview() {
    CourseTimeSection(
        dayOfWeek = DayOfWeek.MONDAY,
        onDayChange = {},
        startPeriod = 1,
        duration = 3,
        maxPeriods = 12,
        onPeriodChange = { _, _ -> },
        conflictingCourses = listOf()
    )
}


@Composable
fun CourseTimeSection(
    dayOfWeek: DayOfWeek,
    onDayChange: (DayOfWeek) -> Unit,
    startPeriod: Int,
    duration: Int,
    maxPeriods: Int,
    onPeriodChange: (Int, Int) -> Unit,
    conflictingCourses: List<Course>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.class_time),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        DayOfWeekSelector(dayOfWeek, onDayChange)

        PeriodSelector(startPeriod, duration, maxPeriods, onPeriodChange)

        if (conflictingCourses.isNotEmpty()) {
            val conflictValue = when (conflictingCourses.size) {
                1 -> conflictingCourses.first().name
                2 -> conflictingCourses.first().name + ", " + conflictingCourses.last().name
                else -> {
                    val firstName = conflictingCourses.first().name + ", " + conflictingCourses[1].name
                    val remainingCount = conflictingCourses.size - 2

                    // 💡 统一口径：传入第一个名字和剩余个数
                    stringResource(R.string.course_conflict_format, firstName, remainingCount)
                }
            }

            ErrorBox(
                text = stringResource(
                    R.string.course_already_existed_during_this_period,
                    conflictValue
                )
            )
        }
    }
}


@Preview
@Composable
fun PeriodSelectorPreview() {
    PeriodSelector(startPeriod = 1, duration = 3, maxPeriods = 12, onValueChange = { _, _ -> })
}

@Composable
fun PeriodSelector(
    startPeriod: Int,
    duration: Int,
    maxPeriods: Int,
    onValueChange: (Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.starting_period),
                modifier = Modifier.width(80.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            NumberStepper(
                value = startPeriod,
                range = 1..maxPeriods,
                onValueChange = { newStart ->
                    val newDuration = if (newStart + duration - 1 > maxPeriods) {
                        maxPeriods - newStart + 1
                    } else duration
                    onValueChange(newStart, newDuration)
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.lasting_periods),
                modifier = Modifier.width(80.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            val maxAllowedDuration = maxPeriods - startPeriod + 1
            NumberStepper(
                value = duration,
                range = 1..maxAllowedDuration,
                onValueChange = { onValueChange(startPeriod, it) }
            )
        }
    }
}


@Preview
@Composable
fun NumberStepperPreview() {
    NumberStepper(value = 5, range = 1..10, onValueChange = {})
}

@Composable
fun NumberStepper(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 4.dp) // 增加一个浅色背景底座，大屏下更易识别
    ) {
        IconButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
            Icon(
                Icons.Default.RemoveCircleOutline,
                contentDescription = stringResource(R.string.sub)
            )
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium, // 使用 Medium 更有分量
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        IconButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = stringResource(R.string.plus))
        }
    }
}
