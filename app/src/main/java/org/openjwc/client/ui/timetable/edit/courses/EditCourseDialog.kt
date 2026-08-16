package org.openjwc.client.ui.timetable.edit.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.openjwc.client.R
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.SemesterConfig
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsTextFieldWidget
import org.openjwc.client.ui.theme.courseBackgroundColors
import org.openjwc.client.ui.timetable.edit.components.CardItem
import org.openjwc.client.ui.timetable.edit.components.WarningBox
import org.openjwc.client.viewmodels.EditCourseViewModel
import java.time.DayOfWeek

@Preview
@Composable
fun EditCourseDialogPreview() {
    EditCourseDialog(
        tableMetadata = TableMetadata(
            tableName = "测试表",
            semesterConfig = SemesterConfig.default(),
            isCurrent = true
        ),
        existingCourses = emptyList(),
        onDismiss = {},
        onSave = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCourseDialog(
    tableMetadata: TableMetadata,
    existingCourses: List<Course>,
    currentCourseId: Long = 0L,
    initialDay: DayOfWeek? = null,
    initialStartPeriod: Int? = null,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit
) {
    val viewModel = remember(currentCourseId) {
        EditCourseViewModel(
            tableMetadata = tableMetadata,
            existingCourses = existingCourses,
            currentCourseId = currentCourseId,
            initialDay = initialDay,
            initialStartPeriod = initialStartPeriod
        )
    }
    var showCustomWeekPicker by remember { mutableStateOf(false) }

    // 文本字段唯一事实来源，保存时才写回 ViewModel
    val nameState = remember { TextFieldState(viewModel.name) }
    val teacherState = remember { TextFieldState(viewModel.teacher) }
    val locationState = remember { TextFieldState(viewModel.location) }
    val noteState = remember { TextFieldState(viewModel.note) }

    val nameError = if (nameState.text.isBlank()) stringResource(R.string.table_name_cannot_be_empty) else ""

    if (showCustomWeekPicker) {
        CustomWeekPickerDialog(
            totalWeeks = tableMetadata.semesterConfig.weeks,
            initialWeeks = viewModel.weekRule,
            onDismiss = { showCustomWeekPicker = false },
            onConfirm = { selectedWeeks ->
                viewModel.weekRule = selectedWeeks
                showCustomWeekPicker = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth > 600.dp
            val surfaceModifier = if (isWideScreen) {
                Modifier.fillMaxHeight().width(460.dp).align(Alignment.CenterEnd)
            } else {
                Modifier.fillMaxWidth(0.94f).heightIn(max = maxHeight * 0.9f).align(Alignment.Center)
            }

            Surface(
                modifier = surfaceModifier,
                shape = if (isWideScreen) RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp) else RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (currentCourseId == 0L) stringResource(R.string.add_new_course) else stringResource(R.string.edit_course),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                            },
                            actions = {
                                TextButton(
                                    onClick = {
                                        // 写回 ViewModel（保留名称自动配色逻辑）
                                        viewModel.onNameChange(nameState.text.toString())
                                        viewModel.teacher = teacherState.text.toString()
                                        viewModel.location = locationState.text.toString()
                                        viewModel.note = noteState.text.toString()
                                        onSave(viewModel.getResultCourse())
                                    },
                                    enabled = nameState.text.isNotBlank() && !viewModel.isTimeConflict
                                ) {
                                    Text(
                                        if (viewModel.isTimeConflict) stringResource(R.string.time_conflict)
                                        else stringResource(R.string.save),
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            )
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                        // —— 基本信息 ——
                        SegmentedColumn(title = stringResource(R.string.basic_information)) {
                            CourseBasicInfoFields(
                                nameState = nameState,
                                teacherState = teacherState,
                                locationState = locationState
                            )
                        }

                        // —— 上课时间 ——
                        SegmentedColumn(title = stringResource(R.string.class_time)) {
                            item {
                                CardItem {
                                    CourseTimeSection(
                                        dayOfWeek = viewModel.dayOfWeek,
                                        onDayChange = { viewModel.dayOfWeek = it },
                                        startPeriod = viewModel.startPeriod,
                                        duration = viewModel.duration,
                                        maxPeriods = tableMetadata.semesterConfig.periods.size,
                                        onPeriodChange = { s, d ->
                                            viewModel.startPeriod = s
                                            viewModel.duration = d
                                        },
                                        conflictingCourses = viewModel.conflictingCourses
                                    )
                                }
                            }
                        }

                        // 时间冲突提示
                        if (viewModel.conflictingCourses.isNotEmpty()) {
                            val conflictValue = when (viewModel.conflictingCourses.size) {
                                1 -> viewModel.conflictingCourses.first().name
                                2 -> viewModel.conflictingCourses.first().name + ", " + viewModel.conflictingCourses.last().name
                                else -> {
                                    val firstName = viewModel.conflictingCourses.first().name + ", " + viewModel.conflictingCourses[1].name
                                    val remainingCount = viewModel.conflictingCourses.size - 2
                                    stringResource(R.string.course_conflict_format, firstName, remainingCount)
                                }
                            }
                            WarningBox(
                                stringResource(
                                    R.string.course_already_existed_during_this_period,
                                    conflictValue
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        // —— 课程颜色 ——
                        SegmentedColumn(title = stringResource(R.string.course_color)) {
                            item {
                                CardItem {
                                    ColorPickerRow(
                                        selectedIndex = viewModel.colorIndex,
                                        presetColors = courseBackgroundColors
                                    ) { index ->
                                        viewModel.colorIndex = index
                                        viewModel.hasManuallyChangedColor = true
                                    }
                                }
                            }
                        }

                        // —— 周次规则 ——
                        SegmentedColumn(title = stringResource(R.string.week_range)) {
                            item {
                                CardItem {
                                    CourseWeekRuleSection(
                                        weekRule = viewModel.weekRule,
                                        totalWeeks = tableMetadata.semesterConfig.weeks,
                                        onRuleChange = { viewModel.weekRule = it },
                                        onCustomClick = { showCustomWeekPicker = true }
                                    )
                                }
                            }
                        }

                        // —— 备注 ——
                        SegmentedColumn(title = stringResource(R.string.notes)) {
                            item {
                                SettingsTextFieldWidget(
                                    state = noteState,
                                    title = "",
                                    useLabelAsPlaceholder = true,
                                    placeholder = stringResource(R.string.notes_hint),
                                    lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 8)
                                )
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}
