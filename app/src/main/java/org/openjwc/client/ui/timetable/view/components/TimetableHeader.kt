package org.openjwc.client.ui.timetable.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TimetableHeader(
    modifier: Modifier = Modifier,
    currentWeek: Int,
    startDate: LocalDate,
    sortedVisibleDays: List<DayOfWeek>,
    timeLabelWidth: Dp,
    titleHeight: Dp,
    locale: Locale = Locale.getDefault(),
    showDate: Boolean = true,
) {
    val weekDates = remember(currentWeek, startDate) {
        val weekStart = startDate.plusWeeks((currentWeek - 1).toLong())
            .with(DayOfWeek.MONDAY)
        (0..6).associateBy { DayOfWeek.of(it + 1) }
            .mapValues { weekStart.plusDays((it.key.value - 1).toLong()) }
    }

    val today = remember { LocalDate.now() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(titleHeight)
    ) {
        // 左侧节次栏的上方空白占位
        Box(modifier = Modifier.width(timeLabelWidth))

        sortedVisibleDays.forEach { day ->
            val dateOfThisDay = weekDates[day] ?: today
            val isToday = dateOfThisDay == today

            // 💡 2. 使用 Box + 权重，确保每一列严格对齐网格
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 星期文字
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (showDate) {
                    // 💡 3. 日期部分：增加“胶囊”背景或圆形高亮（MD3 典型风格）
                    Surface(
                        shape = CircleShape,
                        color = if (isToday) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        contentColor = if (isToday) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.outline
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            text = "${dateOfThisDay.monthValue}/${dateOfThisDay.dayOfMonth}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
