package org.openjwc.client.ui.timetable.view.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openjwc.client.data.models.SemesterConfig
import java.time.DayOfWeek

/**
 * 课表网格背景 + 空槽位点击区域。
 * 用单个 Canvas 绘制网格线、单个 pointerInput 处理点击，替代原来每个格子一个 Box。
 */
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
    val periodCount = config.periods.size
    val dayCount = sortedVisibleDays.size
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(periodHeight * periodCount)
    ) {
        // 左侧节次标签列
        Column(Modifier.width(timeLabelWidth)) {
            config.periods.forEachIndexed { index, period ->
                val isActive = index == activePeriodIndex
                Box(
                    modifier = Modifier
                        .height(periodHeight)
                        .fillMaxWidth()
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PeriodLabel(period = period, isActive = isActive, showPeriodTime = showPeriodTime)
                }
            }
        }

        // 网格区域：Canvas 画线 + 单点 tap 定位空槽
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .drawBehind {
                    val totalW = size.width
                    val totalH = size.height
                    val colW = totalW / dayCount
                    val rowH = totalH / periodCount
                    val stroke = 0.5.dp.toPx()

                    // 活跃节次行高亮
                    if (activePeriodIndex in 0 until periodCount) {
                        drawRect(
                            color = primaryContainer.copy(alpha = 0.3f),
                            topLeft = Offset(0f, activePeriodIndex * rowH),
                            size = Size(totalW, rowH)
                        )
                    }

                    // 水平线
                    for (i in 0..periodCount) {
                        val y = i * rowH
                        drawLine(lineColor, Offset(0f, y), Offset(totalW, y), strokeWidth = stroke)
                    }
                    // 垂直线
                    for (i in 0..dayCount) {
                        val x = i * colW
                        drawLine(lineColor, Offset(x, 0f), Offset(x, totalH), strokeWidth = stroke)
                    }
                }
                .pointerInput(dayCount, periodCount) {
                    detectTapGestures { offset ->
                        val colW = size.width / dayCount
                        val rowH = size.height / periodCount
                        val dayIndex = (offset.x / colW).toInt().coerceIn(0, dayCount - 1)
                        val periodIndex = (offset.y / rowH).toInt().coerceIn(0, periodCount - 1)
                        onEmptySlotClick(
                            sortedVisibleDays[dayIndex],
                            config.periods[periodIndex].index
                        )
                    }
                }
        )
    }
}
