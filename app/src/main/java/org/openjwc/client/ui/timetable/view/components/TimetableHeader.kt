package org.openjwc.client.ui.timetable.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    val today = remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDate.now()
            if (now != today.value) today.value = now
            val millisToMidnight = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                java.time.LocalDate.now().plusDays(1).atStartOfDay()
            ).toMillis()
            delay(millisToMidnight)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(titleHeight)
    ) {
        // 左侧节次栏的上方空白占位
        Box(
            modifier = Modifier
                .width(timeLabelWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
        )

        sortedVisibleDays.forEach { day ->
            val dateOfThisDay = weekDates[day] ?: today.value
            val isToday = dateOfThisDay == today.value

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        else Color.Transparent
                    ),
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
                    // MD3E 胶囊形今天高亮
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
