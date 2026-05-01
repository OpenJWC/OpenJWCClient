package org.openjwc.client.ui.timetable.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R

@Preview
@Composable
fun TimetableTopAppBarPreview() {
    TimetableTopAppBar(
        timetableName = "测试课表",
        currentWeek = 1,
        onTitleClick = {},
        onPrev = {},
        onNext = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableTopAppBar(
    // 建议直接传入 Metadata 对象，或者保持 String 并在调用处传 tableMetadata.tableName
    timetableName: String,
    currentWeek: Int,
    onTitleClick: () -> Unit,
    canPrev: Boolean = true,
    canNext: Boolean = true,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    TopAppBar(
        title = {
            // 点击区域：包含标题和下拉图标
            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, // 标题点击通常不需要大面积涟漪，保持干净
                        onClick = onTitleClick
                    ),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timetableName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1 // 防止名称过长挤乱布局
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.switch_timetable),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    // 确保你的 strings.xml 中 current_week 是 "第 %1\$d 周"
                    text = stringResource(R.string.current_week, currentWeek),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            // 组合周次切换按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev, enabled = canPrev) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.last_week)
                    )
                }
                // 这里可以加一个微小的分割线或间距
                IconButton(onClick = onNext, enabled = canNext) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.next_week)
                    )
                }
            }
        }
    )
}