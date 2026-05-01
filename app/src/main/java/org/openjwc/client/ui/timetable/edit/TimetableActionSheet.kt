package org.openjwc.client.ui.timetable.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.ui.timetable.edit.components.ActionItem


@Preview
@Composable
fun TimetableActionSheetPreview() {
    TimetableActionSheet(timetableCount = 1, onDismissRequest = {}, onActionClick = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableActionSheet(
    timetableCount: Int,
    onDismissRequest: () -> Unit = {},
    onActionClick: (TimetableAction) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // 强制直接全展开，避免半屏状态下的手势冲突
    )
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        // 定义操作类型枚举，方便维护
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp) // 预留底部安全距离，防止遮挡系统手势线
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部的“小横条”指示器在 ModalBottomSheet 中是自带的，这里直接写标题
            Text(
                text = stringResource(
                    R.string.manage_timetable
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            if (timetableCount > 0) {
                // 1. 切换课表 (有表时最重要的功能)
                ActionItem(
                    title = stringResource(R.string.switch_timetable),
                    subtitle = stringResource(R.string.switch_between),
                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                    onClick = {
                        onDismissRequest()
                        onActionClick(TimetableAction.Switch)
                    }
                )

                ActionItem(
                    title = stringResource(R.string.edit_semester_config), // 建议使用你在 TableConfigDialog 里的那个 String
                    subtitle = stringResource(R.string.edit_config_hint), // 或者对应的 R.string
                    icon = Icons.Default.EditCalendar, // 这里的图标可以按需调整
                    onClick = {
                        onDismissRequest()
                        onActionClick(TimetableAction.EditConfig)
                    }
                )

                // 2. 手动添加课程
                ActionItem(
                    title = stringResource(R.string.add_single_course),
                    subtitle = stringResource(R.string.add_manually),
                    icon = Icons.Default.MoreTime,
                    onClick = {
                        onDismissRequest()
                        onActionClick(TimetableAction.AddCourse)
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), // 增加水平边距
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant // 使用更细腻的边框色
                )
            }

            // --- 第二组：获取/创建新课表 ---

            // 3. 自动导入
            ActionItem(
                title = stringResource(R.string.import_from),
                subtitle = stringResource(R.string.auto_import),
                icon = Icons.Default.CloudDownload,
                onClick = {
                    onDismissRequest()
                    onActionClick(TimetableAction.Import)
                }
            )

            // 4. 从空白创建
            ActionItem(
                title = stringResource(R.string.create_a_blank_timetable),
                subtitle = stringResource(R.string.plan_from_scratch),
                icon = Icons.Default.LibraryAdd,
                onClick = {
                    onDismissRequest()
                    onActionClick(TimetableAction.CreateEmpty)
                }
            )
            if (timetableCount > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), // 增加水平边距
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant // 使用更细腻的边框色
                )

                ActionItem(
                    title = stringResource(R.string.delete_timetable),
                    subtitle = stringResource(R.string.delete_permanently_warning),
                    isDanger = true,
                    icon = Icons.Default.DeleteForever,
                    onClick = {
                        onDismissRequest()
                        onActionClick(TimetableAction.Delete)
                    }
                )
            }
        }
    }
}

enum class TimetableAction {
    Import, AddCourse, Switch, CreateEmpty, Delete, EditConfig, AddShortCut
}