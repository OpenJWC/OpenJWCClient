package org.openjwc.client.ui.timetable.edit.tables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.ui.timetable.edit.components.CardItem
import org.openjwc.client.ui.timetable.edit.components.WarningBox

@Composable
fun WeekSlider(
    weeks: Int,
    initialWeeks: Int?,
    maxWeeks: Int,
    onValueChange: (Int) -> Unit
) {
    val hasChanged = initialWeeks != null && weeks != initialWeeks

    CardItem {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.week_number_in_a_semester),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.total_week_number, weeks),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasChanged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = weeks.toFloat(),
                onValueChange = { floatValue ->
                    onValueChange(floatValue.toInt())
                },
                valueRange = 1f..maxWeeks.toFloat(),
                steps = if (maxWeeks > 2) maxWeeks - 2 else 0,
                modifier = Modifier.fillMaxWidth()
            )

            if (hasChanged) {
                WarningBox(
                    text = if (weeks < initialWeeks)
                        stringResource(R.string.reduce_weeks_warning)
                    else
                        stringResource(R.string.extend_weeks_warning),
                )
            }
        }
    }
}


@Preview
@Composable
fun WeekSliderPreview() {
    WeekSlider(
        weeks = 15,
        initialWeeks = 16,
        maxWeeks = 24,
        onValueChange = {}
    )
}
