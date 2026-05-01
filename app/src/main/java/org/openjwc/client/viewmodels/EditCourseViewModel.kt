package org.openjwc.client.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.ui.theme.courseBackgroundColors
import java.time.DayOfWeek

class EditCourseViewModel(
    private val tableMetadata: TableMetadata,
    private val existingCourses: List<Course>,
    private val currentCourseId: Long = 0L,
    initialDay: DayOfWeek? = null,
    initialStartPeriod: Int? = null
) : ViewModel() {

    private val editingCourse = existingCourses.find { it.id == currentCourseId }

    var name by mutableStateOf(editingCourse?.name ?: "")
    var teacher by mutableStateOf(editingCourse?.teacher ?: "")
    var location by mutableStateOf(editingCourse?.location ?: "")
    var dayOfWeek by mutableStateOf(editingCourse?.dayOfWeek ?: initialDay ?: DayOfWeek.MONDAY)
    var startPeriod by mutableIntStateOf(editingCourse?.startPeriod ?: initialStartPeriod ?: 1)
    var duration by mutableIntStateOf(editingCourse?.duration ?: 2)
    var colorIndex by mutableIntStateOf(
        if (editingCourse != null) {
            courseBackgroundColors.indexOf(editingCourse.color).coerceAtLeast(0)
        } else {
            0
        }
    )
    var weekRule by mutableStateOf(editingCourse?.weekRule ?: (1..tableMetadata.semesterConfig.weeks).toSet())
    var note by mutableStateOf(editingCourse?.note ?: "")

    var hasManuallyChangedColor by mutableStateOf(false)

    val canSave: Boolean get() = name.isNotBlank() && !isTimeConflict

    val conflictingCourses: List<Course>
        get() = existingCourses.filter { other ->
            if (other.id != 0L && other.id == currentCourseId) return@filter false
            
            if (other.dayOfWeek != dayOfWeek) return@filter false
            
            val thisEnd = startPeriod + duration - 1
            val otherEnd = other.startPeriod + other.duration - 1
            val periodOverlap = maxOf(startPeriod, other.startPeriod) <= minOf(thisEnd, otherEnd)
            
            periodOverlap && (weekRule intersect other.weekRule).isNotEmpty()
        }

    val isTimeConflict: Boolean get() = conflictingCourses.isNotEmpty()

    fun onNameChange(newName: String) {
        name = newName
        if (!hasManuallyChangedColor && newName.isNotBlank()) {
            colorIndex = Math.abs(newName.hashCode()) % courseBackgroundColors.size
        }
    }

    fun getResultCourse(): Course {
        return Course(
            id = currentCourseId,
            tableId = tableMetadata.id,
            name = name,
            teacher = teacher,
            location = location,
            dayOfWeek = dayOfWeek,
            startPeriod = startPeriod,
            duration = duration,
            color = courseBackgroundColors[colorIndex],
            weekRule = weekRule,
            note = note
        )
    }
}
