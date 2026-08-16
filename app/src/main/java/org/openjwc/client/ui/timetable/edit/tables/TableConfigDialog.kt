package org.openjwc.client.ui.timetable.edit.tables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.openjwc.client.R
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsTextFieldWidget
import org.openjwc.client.ui.timetable.edit.components.CardItem
import org.openjwc.client.ui.timetable.edit.components.ErrorBox
import org.openjwc.client.ui.timetable.edit.components.WarningBox
import org.openjwc.client.viewmodels.TableConfigViewModel
import java.time.format.DateTimeFormatter

@Preview
@Composable
fun TableConfigDialogPreview() {
    TableConfigDialog(
        onDismiss = {}, onConfirm = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableConfigDialog(
    initialMetadata: TableMetadata? = null,
    onDismiss: () -> Unit,
    maxPeriodInUse: Int = 0,
    onConfirm: (TableMetadata) -> Unit
) {
    val viewModel = remember(initialMetadata) { TableConfigViewModel(initialMetadata) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    var showDatePicker by remember { mutableStateOf(false) }
    var pickingTimeIndex by remember { mutableIntStateOf(-1) }
    var isPickingStartTime by remember { mutableStateOf(true) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = viewModel.selectedDateMillis)

    // 课表名称：TextFieldState 为对话框内唯一事实来源，确认时才写回 viewModel
    val nameState = remember { TextFieldState(viewModel.tableName) }
    val nameError = if (nameState.text.isBlank()) stringResource(R.string.table_name_cannot_be_empty) else ""

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = maxHeight * 0.9f)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp)) {
                    Text(
                        text = stringResource(if (initialMetadata != null) R.string.edit_semester_config else R.string.create_a_blank_timetable),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 16.dp)
                    ) {
                        // —— 基本信息 ——
                        SegmentedColumn {
                            item {
                                SettingsTextFieldWidget(
                                    state = nameState,
                                    title = stringResource(R.string.timetable_name),
                                    error = nameError
                                )
                            }
                            item {
                                DateSelectionCard(
                                    dateText = viewModel.selectedLocalDate.toString(),
                                    onClick = { showDatePicker = true }
                                )
                            }
                            item {
                                WeekSlider(
                                    weeks = viewModel.weeks, onValueChange = { viewModel.weeks = it },
                                    initialWeeks = initialMetadata?.semesterConfig?.weeks,
                                    maxWeeks = 30
                                )
                            }
                            item {
                                ConfigSwitchRow(
                                    label = stringResource(R.string.show_weekends),
                                    checked = viewModel.showWeekend,
                                    onCheckedChange = { viewModel.showWeekend = it }
                                )
                            }
                        }

                        // —— 节次时间 ——
                        SegmentedColumn {
                            item {
                                CardItem(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                                    PeriodHeader(
                                        onAdd = { viewModel.addPeriod() }
                                    )
                                    viewModel.periods.forEachIndexed { index, period ->
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.padding(start = 0.dp)
                                        )
                                        PeriodEditItem(
                                            index = index,
                                            period = period,
                                            isError = viewModel.getPeriodErrorType(index) != null,
                                            timeFormatter = timeFormatter,
                                            onEditStart = { pickingTimeIndex = index; isPickingStartTime = true },
                                            onEditEnd = { pickingTimeIndex = index; isPickingStartTime = false },
                                            onDelete = { viewModel.removePeriod(index) }
                                        )
                                    }
                                }
                            }
                        }

                        if (viewModel.periods.size < maxPeriodInUse) {
                            WarningBox(
                                stringResource(
                                    R.string.period_count_too_small_warning,
                                    viewModel.periods.size,
                                    maxPeriodInUse
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        if (!viewModel.isPeriodsValid) {
                            ErrorBox(
                                stringResource(R.string.time_conflict_warning),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                        Button(
                            onClick = {
                                viewModel.tableName = nameState.text.toString()
                                onConfirm(viewModel.getFinalMetadata())
                            },
                            enabled = nameState.text.isNotBlank() && viewModel.isPeriodsValid
                        ) {
                            Text(stringResource(if (initialMetadata != null) R.string.save else R.string.confirm))
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                datePickerState.selectedDateMillis?.let {
                    viewModel.selectedDateMillis = it
                }
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.confirm)) }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (pickingTimeIndex != -1) {
        val initialTime = if (isPickingStartTime) viewModel.periods[pickingTimeIndex].start else viewModel.periods[pickingTimeIndex].end
        TimePickerDialog(
            initialTime = initialTime,
            onDismiss = { pickingTimeIndex = -1 },
            onConfirm = { newTime ->
                viewModel.updatePeriodTime(pickingTimeIndex, isPickingStartTime, newTime)
                pickingTimeIndex = -1
            }
        )
    }
}
