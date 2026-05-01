package org.openjwc.client.ui.timetable.edit.tables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.data.models.Period
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun PeriodEditItem(
    index: Int,
    period: Period,
    isError: Boolean,
    timeFormatter: DateTimeFormatter,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = if (isError) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.nth_class, index + 1),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onEditStart) { Text(period.start.format(timeFormatter)) }
                Text("-", style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = onEditEnd) { Text(period.end.format(timeFormatter)) }
            }
        }
    }
}

@Preview
@Composable
fun PeriodEditItemPreview() {
    PeriodEditItem(
        index = 0,
        period = Period(1, LocalTime.of(8, 0), LocalTime.of(9, 45)),
        isError = false,
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm"),
        onEditStart = {},
        onEditEnd = {}
    )
}
