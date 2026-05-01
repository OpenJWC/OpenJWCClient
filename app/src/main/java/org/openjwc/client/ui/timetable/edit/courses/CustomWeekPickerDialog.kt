package org.openjwc.client.ui.timetable.edit.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R

@Preview
@Composable
fun CustomWeekPickerDialogPreview() {
    CustomWeekPickerDialog(
        totalWeeks = 7,
        initialWeeks = setOf(1, 3, 5),
        onDismiss = {},
        onConfirm = {}
    )
}
@Composable
fun CustomWeekPickerDialog(
    totalWeeks: Int,
    initialWeeks: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    var tempWeeks by remember { mutableStateOf(initialWeeks) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_class_weeks)) },
        text = {

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. 修复后的网格
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (tempWeeks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.one_week_at_least),
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )}
                }
                WeekGrid(
                    totalWeeks = totalWeeks,
                    selectedWeeks = tempWeeks,
                    onWeekToggle = { weekNum ->
                        // 逻辑：如果已存在则移除，不存在则添加
                        tempWeeks = if (tempWeeks.contains(weekNum)) {
                            tempWeeks - weekNum
                        } else {
                            tempWeeks + weekNum
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 2. 快捷操作区
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    // 间距建议稍微调小，防止总周数多时内容太拥挤
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { tempWeeks = (1..totalWeeks).toSet() }) {
                        Text(stringResource(R.string.select_all))
                    }
                    TextButton(onClick = { tempWeeks = (1..totalWeeks step 2).toSet() }) {
                        Text(stringResource(R.string.odd_week))
                    }
                    TextButton(onClick = { tempWeeks = (2..totalWeeks step 2).toSet() }) {
                        Text(stringResource(R.string.even_week))
                    }
                    TextButton(onClick = { tempWeeks = emptySet() }) {
                        Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tempWeeks) },
                // 💡 只有当选中的周次集合不为空时，按钮才可用
                enabled = tempWeeks.isNotEmpty()
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun WeekGrid(
    totalWeeks: Int,
    selectedWeeks: Set<Int>,
    onWeekToggle: (Int) -> Unit // 修改为带参数的回调
) {
    LazyVerticalGrid(
        // minSize 56dp 可能导致一行只有 3-4 个，48dp 通常比较适合网格布局
        columns = GridCells.Adaptive(minSize = 48.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
        contentPadding = PaddingValues(4.dp),
        // 使用 Grid 自带间距
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(totalWeeks) { index ->
            val weekNum = index + 1
            val isSelected = selectedWeeks.contains(weekNum)

            // 提取颜色逻辑
            val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .clickable { onWeekToggle(weekNum) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = weekNum.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
            }
        }
    }
}