package org.openjwc.client.ui.timetable.view.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import org.openjwc.client.data.models.Period
import org.openjwc.client.ui.timetable.utils.TimetableGridUtils
import java.time.LocalTime

@Composable
fun TimeIndicatorLine(
    periods: List<Period>,
    periodHeight: Dp,
    timeLabelWidth: Dp
) {
    // 1. 每分钟更新一次当前时间
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(60000) // 60秒刷新一次
        }
    }

    // 2. 使用我们刚才抽离的 Utils 计算偏移
    val yOffset = remember(currentTime, periods, periodHeight) {
        TimetableGridUtils.calculateTimeLineOffset(currentTime, periods, periodHeight)
    }

    // 3. 渲染
    yOffset?.let { offset ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = timeLabelWidth) // 避开左侧节次栏
                .offset(y = offset - 1.dp) // 减去高度的一半使其居中
                .zIndex(3f) // 确保在课程卡片之上
        ) {
            // 指示线本体
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )

            // 💡 MD3 细节：在最左侧画一个小圆点，指示感更强
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(x = (-3).dp, y = (-2).dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}