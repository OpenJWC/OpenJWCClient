package org.openjwc.client.ui.timetable.view.state

import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.TableMetadata
import java.time.DayOfWeek

data class TimetableUiState(
    val showActionSheet: Boolean = false,
    val showDetailSheet: Boolean = false,
    val showEditDialog: Boolean = false,
    val showTableConfigDialog: Boolean = false,
    val showTableSelectSheet: Boolean = false,
    val showDeleteTableDialog: Boolean = false,
    val showDeleteCourseDialog: Boolean = false,
    val clickedCourse: Course? = null,
    val editingCourseId: Long = 0L,
    val tableToEdit: TableMetadata? = null,
    
    // 用于新增课程时的初始时间
    val initialDay: DayOfWeek? = null,
    val initialStartPeriod: Int? = null
)
