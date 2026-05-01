package org.openjwc.client.ui.timetable.edit.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.ui.timetable.utils.toDisplayText

@Preview
@Composable
fun CourseWeekRuleSectionPreview() {
    CourseWeekRuleSection(
        weekRule = (1..16).toSet(),
        totalWeeks = 16,
        onRuleChange = {},
        onCustomClick = {}
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseWeekRuleSection(
    weekRule: Set<Int>,
    totalWeeks: Int,
    onRuleChange: (Set<Int>) -> Unit,
    onCustomClick: () -> Unit
) {
    val everyWeeks = (1..totalWeeks).toSet()
    val oddWeeks = (1..totalWeeks).filter { it % 2 != 0 }.toSet()
    val evenWeeks = (1..totalWeeks).filter { it % 2 == 0 }.toSet()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.week_range),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = weekRule.toDisplayText(
                totalWeeks = totalWeeks,
                everyWeekStr = stringResource(R.string.every_week),
                oddWeeksStr = stringResource(R.string.odd_weeks),
                evenWeeksStr = stringResource(R.string.even_weeks),
                customWeeksStr = stringResource(R.string.week_number)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = weekRule == everyWeeks,
                onClick = { onRuleChange(everyWeeks) },
                label = { Text(stringResource(R.string.every_week)) }
            )
            FilterChip(
                selected = weekRule == oddWeeks,
                onClick = { onRuleChange(oddWeeks) },
                label = { Text(stringResource(R.string.odd_weeks)) }
            )
            FilterChip(
                selected = weekRule == evenWeeks,
                onClick = { onRuleChange(evenWeeks) },
                label = { Text(stringResource(R.string.even_weeks)) }
            )
            FilterChip(
                selected = weekRule != everyWeeks && weekRule != oddWeeks && weekRule != evenWeeks,
                onClick = onCustomClick,
                label = { Text(stringResource(R.string.custom_weeks)) },
                leadingIcon = { Icon(Icons.Default.DateRange, null, Modifier.size(18.dp)) }
            )
        }
    }
}
