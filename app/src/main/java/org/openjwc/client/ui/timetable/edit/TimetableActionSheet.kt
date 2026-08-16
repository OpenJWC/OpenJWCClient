package org.openjwc.client.ui.timetable.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget

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
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.manage_timetable),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
            )

            if (timetableCount > 0) {
                // 第一组：当前课表的操作
                SegmentedColumn {
                    item {
                        SettingsJumpPageWidget(
                            icon = Icons.AutoMirrored.Filled.LibraryBooks,
                            title = stringResource(R.string.switch_timetable),
                            description = stringResource(R.string.switch_between),
                            onClick = {
                                onDismissRequest()
                                onActionClick(TimetableAction.Switch)
                            }
                        )
                    }
                    item {
                        SettingsJumpPageWidget(
                            icon = Icons.Default.EditCalendar,
                            title = stringResource(R.string.edit_semester_config),
                            description = stringResource(R.string.edit_config_hint),
                            onClick = {
                                onDismissRequest()
                                onActionClick(TimetableAction.EditConfig)
                            }
                        )
                    }
                    item {
                        SettingsJumpPageWidget(
                            icon = Icons.Default.MoreTime,
                            title = stringResource(R.string.add_single_course),
                            description = stringResource(R.string.add_manually),
                            onClick = {
                                onDismissRequest()
                                onActionClick(TimetableAction.AddCourse)
                            }
                        )
                    }
                }
            }

            // 第二组：获取/创建新课表
            SegmentedColumn {
                item {
                    SettingsJumpPageWidget(
                        icon = Icons.Default.CloudDownload,
                        title = stringResource(R.string.import_from),
                        description = stringResource(R.string.auto_import),
                        onClick = {
                            onDismissRequest()
                            onActionClick(TimetableAction.Import)
                        }
                    )
                }
                item {
                    SettingsJumpPageWidget(
                        icon = Icons.Default.LibraryAdd,
                        title = stringResource(R.string.create_a_blank_timetable),
                        description = stringResource(R.string.plan_from_scratch),
                        onClick = {
                            onDismissRequest()
                            onActionClick(TimetableAction.CreateEmpty)
                        }
                    )
                }
            }

            if (timetableCount > 0) {
                // 第三组：危险操作
                SegmentedColumn {
                    item {
                        SettingsBaseWidget(
                            icon = Icons.Default.DeleteForever,
                            iconColor = MaterialTheme.colorScheme.error,
                            title = stringResource(R.string.delete_timetable),
                            titleStyle = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.error
                            ),
                            description = stringResource(R.string.delete_permanently_warning),
                            descriptionColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            onClick = {
                                onDismissRequest()
                                onActionClick(TimetableAction.Delete)
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class TimetableAction {
    Import, AddCourse, Switch, CreateEmpty, Delete, EditConfig, AddShortCut
}
