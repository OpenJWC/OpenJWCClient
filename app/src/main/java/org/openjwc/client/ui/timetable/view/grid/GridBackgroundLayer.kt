package org.openjwc.client.ui.timetable.view.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openjwc.client.data.models.SemesterConfig
import java.time.DayOfWeek

@Composable
fun GridBackgroundLayer(
    config: SemesterConfig,
    sortedVisibleDays: List<DayOfWeek>,
    periodHeight: Dp,
    timeLabelWidth: Dp,
    activePeriodIndex: Int,
    showPeriodTime: Boolean = true,
    onEmptySlotClick: (DayOfWeek, Int) -> Unit
) {
    Column {
        config.periods.forEachIndexed { index, period ->
            val isActive = index == activePeriodIndex
            val periodNumber = period.index
            Row(modifier = Modifier.height(periodHeight).fillMaxWidth()) {
                // 节次标签区
                Box(
                    modifier = Modifier.width(timeLabelWidth).fillMaxHeight()
                        .background(if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    PeriodLabel(period = period, isActive = isActive, showPeriodTime = showPeriodTime)
                }
                // 网格线 & 点击区域
                sortedVisibleDays.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            .clickable { onEmptySlotClick(day, periodNumber) }
                    )
                }
            }
        }
    }
}
