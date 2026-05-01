package org.openjwc.client.ui.timetable.view.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.data.models.TableMetadata

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
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.switch_timetable),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(tables) { table ->
                val isSelected = table.id == currentTableId
                ListItem(
                    headlineContent = { Text(table.tableName) },
                    supportingContent = {
                        Text(stringResource(
                            R.string.week_starting_date,
                            table.semesterConfig.weeks,
                            table.semesterConfig.startDate
                        ))
                    },
                    leadingContent = {
                        RadioButton(selected = isSelected, onClick = null)
                    },
                    trailingContent = {
                        if (table.isCurrent) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(stringResource(R.string.currently_using)) }
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onTableSelect(table)
                        onDismissRequest()
                    }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.import_from),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onDismissRequest()
                        onImport()
                    }
                )
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.create_a_blank_timetable),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onDismissRequest()
                        onCreateNew()
                    }
                )
            }
        }
    }
}
