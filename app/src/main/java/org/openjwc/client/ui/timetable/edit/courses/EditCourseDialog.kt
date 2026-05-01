package org.openjwc.client.ui.timetable.edit.courses

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.openjwc.client.R
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.SemesterConfig
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.ui.theme.courseBackgroundColors
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
    // 使用 remember 配合 currentCourseId 作为 Key，确保切换编辑对象时 ViewModel 重置
    val viewModel = remember(currentCourseId) {
        EditCourseViewModel(
            tableMetadata = tableMetadata,
            existingCourses = existingCourses,
            currentCourseId = currentCourseId,
            initialDay = initialDay,
            initialStartPeriod = initialStartPeriod
        )
    }

    // 控制自定义周次弹窗的显示
    var showCustomWeekPicker by remember { mutableStateOf(false) }

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
                Modifier.fillMaxHeight().width(420.dp).align(Alignment.CenterEnd)
            } else {
                Modifier.fillMaxWidth(0.92f).heightIn(max = maxHeight * 0.85f).align(Alignment.Center)
            }

            Surface(
                modifier = surfaceModifier,
                shape = if (isWideScreen) RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp) else RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Scaffold(
                    modifier = Modifier.animateContentSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(if (currentCourseId == 0L) stringResource(R.string.add_new_course) else stringResource(R.string.edit_course)) },
                            navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                            actions = {
                                TextButton(
                                    onClick = {
                                        val result = viewModel.getResultCourse()
                                        Log.e("重构检查", "准备发往数据库的 ID: ${result.id}, 名字: ${result.name}")
                                        onSave(result)},
                                    enabled = viewModel.canSave
                                ) {
                                    Text(if (viewModel.isTimeConflict) stringResource(R.string.time_conflict) else stringResource(R.string.save))
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 基础信息
                        CourseBasicInfoFields(
                            name = viewModel.name,
                            onNameChange = { viewModel.onNameChange(it) },
                            teacher = viewModel.teacher,
                            onTeacherChange = { viewModel.teacher = it },
                            location = viewModel.location,
                            onLocationChange = { viewModel.location = it }
                        )

                        HorizontalDivider(modifier = Modifier.alpha(0.3f))

                        // 时间选择
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

                        // 颜色选择
                        ColorPickerRow(
                            selectedIndex = viewModel.colorIndex,
                            presetColors = courseBackgroundColors
                        ) { index ->
                            viewModel.colorIndex = index
                            viewModel.hasManuallyChangedColor = true
                        }

                        HorizontalDivider(modifier = Modifier.alpha(0.3f))

                        // 周次规则
                        CourseWeekRuleSection(
                            weekRule = viewModel.weekRule,
                            totalWeeks = tableMetadata.semesterConfig.weeks,
                            onRuleChange = { viewModel.weekRule = it },
                            onCustomClick = { showCustomWeekPicker = true }
                        )

                        // 备注
                        OutlinedTextField(
                            value = viewModel.note,
                            onValueChange = { viewModel.note = it },
                            label = { Text(stringResource(R.string.notes)) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            minLines = 3
                        )

                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}
