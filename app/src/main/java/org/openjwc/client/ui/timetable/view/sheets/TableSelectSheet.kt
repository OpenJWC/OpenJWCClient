package org.openjwc.client.ui.timetable.view.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsBaseWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableSelectSheet(
    tables: List<TableMetadata>,
    currentTableId: Long,
    onTableSelect: (TableMetadata) -> Unit,
    onCreateNew: () -> Unit,
    onImport: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.switch_timetable),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
            )

            SegmentedColumn {
                tables.forEach { table ->
                    item(key = table.id) {
                        val isSelected = table.id == currentTableId
                        SettingsBaseWidget(
                            leadingContent = {
                                RadioButton(selected = isSelected, onClick = null)
                            },
                            title = table.tableName,
                            titleStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            description = stringResource(
                                R.string.week_starting_date,
                                table.semesterConfig.weeks,
                                table.semesterConfig.startDate
                            ),
                            selected = isSelected,
                            onClick = {
                                onTableSelect(table)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }

            SegmentedColumn {
                item {
                    SettingsBaseWidget(
                        icon = Icons.Default.CloudDownload,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.import_from),
                        titleStyle = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        onClick = {
                            onDismissRequest()
                            onImport()
                        }
                    )
                }
                item {
                    SettingsBaseWidget(
                        icon = Icons.Default.Add,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.create_a_blank_timetable),
                        titleStyle = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        ),
                        onClick = {
                            onDismissRequest()
                            onCreateNew()
                        }
                    )
                }
            }
        }
    }
}
