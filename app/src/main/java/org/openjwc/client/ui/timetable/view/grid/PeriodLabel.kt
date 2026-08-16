package org.openjwc.client.ui.timetable.view.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.data.models.Period
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Preview
@Composable
fun PeriodLabelPreview() {
    PeriodLabel(period = Period(1, LocalTime.of(8, 0), LocalTime.of(9, 30)))
}


@Composable
fun PeriodLabel(
    period: Period,
    isActive: Boolean = false,
    showPeriodTime: Boolean = true
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val mainColor =
        if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val subColor =
        if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
        else MaterialTheme.colorScheme.onSurfaceVariant
    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = period.index.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = mainColor,
            fontWeight = fontWeight,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else androidx.compose.ui.graphics.Color.Transparent
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        if (showPeriodTime) {
            Text(
                text = period.start.format(timeFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = subColor,
                fontWeight = fontWeight
            )
            Text(
                text = period.end.format(timeFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = subColor,
                fontWeight = fontWeight
            )
        }
    }
}
