package org.openjwc.client.ui.timetable.view.components

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.ui.timetable.edit.TimetableAction
import org.openjwc.client.ui.timetable.edit.TimetableActionSheet
import org.openjwc.client.ui.timetable.edit.courses.DeleteCourseDialog
import org.openjwc.client.ui.timetable.edit.courses.EditCourseDialog
import org.openjwc.client.ui.timetable.edit.tables.DeleteTableDialog
import org.openjwc.client.ui.timetable.edit.tables.TableConfigDialog
import org.openjwc.client.ui.timetable.view.sheets.CourseDetailSheet
import org.openjwc.client.ui.timetable.view.sheets.TableSelectSheet
import org.openjwc.client.ui.timetable.view.state.TimetableUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableOverlayHost(
    windowSizeClass: WindowSizeClass,
    state: TimetableUiState,
    onStateChange: (TimetableUiState) -> Unit,
    currentTable: TableMetadata?,
    maxPeriodInUse: Int,
    currentWeek: Int,
    currentTableCourses: List<Course>,
    allTables: List<TableMetadata>,
    onImportRequest: () -> Unit,
    onSaveCourse: (Course) -> Unit,
    onDeleteCourse: (Long) -> Unit,
    onCreateTable: (TableMetadata) -> Unit,
    onUpdateTable: (TableMetadata) -> Unit,
    onDeleteTable: (Long) -> Unit,
    onSwitchTable: (Long) -> Unit
) {
    // A. FAB 操作菜单
    if (state.showActionSheet) {
        TimetableActionSheet(
            onActionClick = { action ->
                val baseState = state.copy(showActionSheet = false)
                when (action) {
                    TimetableAction.Import -> {
                        onImportRequest()
                    }
                    TimetableAction.AddCourse -> {
                        if (currentTable == null) {
                            onStateChange(baseState.copy(showTableConfigDialog = true))
                        } else {
                            onStateChange(baseState.copy(
                                editingCourseId = 0L,
                                showEditDialog = true,
                                initialDay = null,
                                initialStartPeriod = null
                            ))
                        }
                    }
                    TimetableAction.CreateEmpty -> onStateChange(baseState.copy(showTableConfigDialog = true))
                    TimetableAction.Switch -> onStateChange(baseState.copy(showTableSelectSheet = true))
                    TimetableAction.Delete -> onStateChange(baseState.copy(showDeleteTableDialog = true))
                    TimetableAction.EditConfig -> {
                        onStateChange(baseState.copy(tableToEdit = currentTable, showTableConfigDialog = true))
                    }
                    else -> {}
                }
            },
            onDismissRequest = { onStateChange(state.copy(showActionSheet = false)) },
            timetableCount = allTables.size
        )
    }

    // B. 课程详情
    if (state.showDetailSheet && state.clickedCourse != null) {
        val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        CourseDetailSheet(
            windowSizeClass = windowSizeClass,
            course = state.clickedCourse,
            onDismissRequest = { onStateChange(state.copy(showDetailSheet = false)) },
            onEdit = {
                onStateChange(state.copy(
                    editingCourseId = state.clickedCourse.id,
                    showDetailSheet = false,
                    showEditDialog = true
                ))
            },
            onDelete = {
                onStateChange(state.copy(showDeleteCourseDialog = true, showDetailSheet = false))
            },
            currentWeek = currentWeek,
            totalWeeks = currentTable?.semesterConfig?.weeks ?: 16,
            sheetState = detailSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        )
    }

    // C. 编辑/新增课程对话框
    if (state.showEditDialog) {
        currentTable?.let { metadata ->
            EditCourseDialog(
                tableMetadata = metadata,
                existingCourses = currentTableCourses,
                currentCourseId = state.editingCourseId,
                initialDay = state.initialDay,
                initialStartPeriod = state.initialStartPeriod,
                onDismiss = { onStateChange(state.copy(showEditDialog = false)) },
                onSave = { updatedCourse ->
                    onSaveCourse(updatedCourse)
                    onStateChange(state.copy(showEditDialog = false))
                }
            )
        }
    }

    // D. 课表配置对话框 (修改/创建课表)
    if (state.showTableConfigDialog) {
        TableConfigDialog(
            initialMetadata = state.tableToEdit,
            onDismiss = { onStateChange(state.copy(showTableConfigDialog = false, tableToEdit = null)) },
            maxPeriodInUse = maxPeriodInUse,
            onConfirm = { metadata ->
                if (state.tableToEdit == null) onCreateTable(metadata)
                else onUpdateTable(metadata)
                onStateChange(state.copy(showTableConfigDialog = false, tableToEdit = null))
            }
        )
    }

    // E. 课表切换选择器
    if (state.showTableSelectSheet) {
        TableSelectSheet(
            tables = allTables,
            currentTableId = currentTable?.id ?: -1L,
            onTableSelect = { table ->
                onSwitchTable(table.id)
                onStateChange(state.copy(showTableSelectSheet = false))
            },
            onCreateNew = { onStateChange(state.copy(showTableConfigDialog = true, showTableSelectSheet = false)) },
            onImport = onImportRequest,
            onDismissRequest = { onStateChange(state.copy(showTableSelectSheet = false)) }
        )
    }

    // F. 删除确认对话框 (课表)
    if (state.showDeleteTableDialog) {
        currentTable?.let { table ->
            DeleteTableDialog(
                tableName = table.tableName,
                onDismiss = { onStateChange(state.copy(showDeleteTableDialog = false)) },
                onConfirm = {
                    onDeleteTable(table.id)
                    onStateChange(state.copy(showDeleteTableDialog = false))
                }
            )
        }
    }

    // G. 删除确认对话框 (课程)
    if (state.showDeleteCourseDialog) {
        state.clickedCourse?.let { course ->
            DeleteCourseDialog(
                courseName = course.name,
                onDismiss = { onStateChange(state.copy(showDeleteCourseDialog = false)) },
                onConfirm = {
                    onDeleteCourse(course.id)
                    onStateChange(state.copy(showDeleteCourseDialog = false))
                }
            )
        }
    }
}