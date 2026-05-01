package org.openjwc.client.ui.timetable.edit.tables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.ui.timetable.edit.components.WarningBox

@Composable
fun WeekSlider(
    weeks: Int,
    initialWeeks: Int?,
    maxWeeks: Int,
    onValueChange: (Int) -> Unit // 保持 Int 接口
) {
    // 逻辑判断
    val hasChanged = initialWeeks != null && weeks != initialWeeks

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.week_number_in_a_semester), style = MaterialTheme.typography.labelMedium)
            Text(
                stringResource(R.string.total_week_number, weeks),
                color = if (hasChanged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = weeks.toFloat(),
            // 💡 关键修改：在这里进行转换，而不是强转函数类型
            onValueChange = { floatValue ->
                onValueChange(floatValue.toInt())
            },
            valueRange = 1f..maxWeeks.toFloat(),
            // steps 应该是总数减 1
            steps = if (maxWeeks > 1) maxWeeks - 1 else 0
        )

        if (hasChanged) {
            Spacer(modifier = Modifier.padding(4.dp))
            WarningBox(
                text = if (weeks < initialWeeks)
                    stringResource(R.string.reduce_weeks_warning)
                else
                    stringResource(R.string.extend_weeks_warning),
            )
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