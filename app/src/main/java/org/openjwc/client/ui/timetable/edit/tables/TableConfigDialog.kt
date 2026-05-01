package org.openjwc.client.ui.timetable.edit.tables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.data.models.TableMetadata
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initialMetadata != null) R.string.edit_semester_config else R.string.create_a_blank_timetable)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = viewModel.tableName,
                    onValueChange = { viewModel.tableName = it },
                    label = { Text(stringResource(R.string.timetable_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = viewModel.tableName.trim().isEmpty(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                if (viewModel.tableName.trim().isEmpty()) {
                    ErrorBox(stringResource(R.string.table_name_cannot_be_empty))
                }

                DateSelectionCard(
                    dateText = viewModel.selectedLocalDate.toString(),
                    onClick = { showDatePicker = true }
                )

                WeekSlider(
                    weeks = viewModel.weeks, onValueChange = { viewModel.weeks = it },
                    initialWeeks = initialMetadata?.semesterConfig?.weeks,
                    maxWeeks = 30
                )

                ConfigSwitchRow(
                    label = stringResource(R.string.show_weekends),
                    checked = viewModel.showWeekend,
                    onCheckedChange = { viewModel.showWeekend = it }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                PeriodHeader(
                    onAdd = { viewModel.addPeriod() },
                    onDelete = { viewModel.removeLastPeriod() }
                )

                if (viewModel.periods.size < maxPeriodInUse) {
                    WarningBox(
                        stringResource(
                            R.string.period_count_too_small_warning,
                            viewModel.periods.size,
                            maxPeriodInUse
                        )
                    )
                }

                if (!viewModel.isPeriodsValid) {
                    ErrorBox(stringResource(R.string.time_conflict_warning))
                }

                for ((index, period) in viewModel.periods.withIndex()) {
                    PeriodEditItem(
                        index = index,
                        period = period,
                        isError = viewModel.getPeriodErrorType(index) != null,
                        timeFormatter = timeFormatter,
                        onEditStart = { pickingTimeIndex = index; isPickingStartTime = true },
                        onEditEnd = { pickingTimeIndex = index; isPickingStartTime = false }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(viewModel.getFinalMetadata()) },
                enabled = viewModel.tableName.isNotBlank() && viewModel.isPeriodsValid
            ) {
                Text(stringResource(if (initialMetadata != null) R.string.save else R.string.confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )

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
            DatePicker(state = datePickerState, showModeToggle = false, title = null, headline = null)
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
