package org.openjwc.client.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.SemesterConfig
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.data.repository.CourseRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.log.Logger
import org.openjwc.client.ui.timetable.view.state.TimetableUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 课表界面显示偏好
 */
data class TimetableDisplayPrefs(
    val showTimeline: Boolean = true,
    val showDate: Boolean = true,
    val showPeriodTime: Boolean = true,
    val showNonCurrentWeek: Boolean = true
)

class TimetableViewModel(
    private val courseRepository: CourseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val TAG = "TimetableViewModel"

    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val currentTable = courseRepository.currentTable.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = null
    )

    val allTables = courseRepository.allTables.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val currentTableCourses = courseRepository.currentCourses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val currentMaxPeriodInUse = currentTableCourses.map { courses ->
        courses.maxOfOrNull { it.startPeriod + it.duration - 1 } ?: 0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = 0
    )

    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    var isInternalWeekUpdate by mutableStateOf(false)
        private set

    private val _activePeriodIndex = MutableStateFlow(-1)
    val activePeriodIndex = _activePeriodIndex.asStateFlow()

    val displayPrefs: StateFlow<TimetableDisplayPrefs> = settingsRepository.userSettings
        .map { settings ->
            TimetableDisplayPrefs(
                showTimeline = settings.showTimeline,
                showDate = settings.showDate,
                showPeriodTime = settings.showPeriodTime,
                showNonCurrentWeek = settings.showNonCurrentWeek
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = TimetableDisplayPrefs()
        )

    var isImporting by mutableStateOf(false)
        private set

    private var timerJob: Job? = null
    private var lastTableId: Long? = null

    init {
        viewModelScope.launch {
            combine(currentTable, allTables) { _, _ -> true }.first()
            _isReady.value = true
        }

        viewModelScope.launch {
            currentTable.collect { table ->
                if (table?.id != lastTableId) {
                    lastTableId = table?.id
                    table?.semesterConfig?.let { config ->
                        val calculated = config.calculateCurrentWeek() ?: 1
                        _currentWeek.value = calculated
                        isInternalWeekUpdate = true
                    }
                }
            }
        }
        startPeriodTimer()
    }

    private fun startPeriodTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val table = currentTable.value
                if (table != null) {
                    val now = LocalTime.now()
                    val index = table.semesterConfig.periods.indexOfFirst { period ->
                        !now.isBefore(period.start) && !now.isAfter(period.end)
                    }
                    _activePeriodIndex.value = index
                }
                delay(60000)
            }
        }
    }

    fun setWeek(week: Int, fromPager: Boolean = false) {
        _currentWeek.value = week
        if (!fromPager) isInternalWeekUpdate = true
    }

    fun consumeInternalUpdate() {
        isInternalWeekUpdate = false
    }

    fun switchTable(tableId: Long) {
        viewModelScope.launch {
            courseRepository.setCurrentTable(tableId)
            val table = courseRepository.getTableById(tableId)
            table?.semesterConfig?.let { config ->
                syncToActualWeek(config)
                isInternalWeekUpdate = true
            }
        }
    }

    fun createTable(table: TableMetadata) {
        viewModelScope.launch {
            val newId = courseRepository.saveTable(table)
            courseRepository.setCurrentTable(newId)
            syncToActualWeek(table.semesterConfig)
            isInternalWeekUpdate = true
            Logger.d(TAG, "Created and switched to new table ID: $newId")
        }
    }

    private fun syncToActualWeek(config: SemesterConfig) {
        val today = LocalDate.now()
        val startMonday = config.startDate.with(java.time.DayOfWeek.MONDAY)
        val daysBetween = ChronoUnit.DAYS.between(startMonday, today)
        val calculatedWeek = (daysBetween / 7).toInt() + 1

        _currentWeek.value = when {
            today.isBefore(config.startDate) -> 1
            else -> calculatedWeek.coerceIn(1, config.weeks)
        }
    }

    fun deleteTable(tableId: Long) = viewModelScope.launch {
        val currentList = allTables.first()
        val isDeletingCurrent = (currentTable.value?.id == tableId)
        courseRepository.deleteTable(tableId)
        if (isDeletingCurrent) {
            val remaining = currentList.filter { it.id != tableId }
            if (remaining.isNotEmpty()) {
                val nextTable = remaining.first()
                courseRepository.setCurrentTable(nextTable.id)
                syncToActualWeek(nextTable.semesterConfig)
            } else {
                _currentWeek.value = 1
            }
        }
    }

    fun updateTable(metadata: TableMetadata) = viewModelScope.launch {
        val tableToUpdate = if (metadata.id == 0L) {
            currentTable.value?.copy(tableName = metadata.tableName, semesterConfig = metadata.semesterConfig)
                ?: metadata
        } else {
            metadata
        }

        courseRepository.updateTable(tableToUpdate)

        if (_currentWeek.value > tableToUpdate.semesterConfig.weeks) {
            _currentWeek.value = tableToUpdate.semesterConfig.weeks
            isInternalWeekUpdate = true
        }

        if (tableToUpdate.id == currentTable.value?.id) {
            lastTableId = tableToUpdate.id
        }

        courseRepository.setCurrentTable(tableToUpdate.id)
        Logger.d(TAG, "Updated table with ID ${tableToUpdate.id}: $tableToUpdate")
    }

    fun saveCourse(course: Course) {
        viewModelScope.launch {
            courseRepository.saveCourse(course)
        }
    }

    fun removeCourse(courseId: Long) {
        viewModelScope.launch {
            courseRepository.deleteCourse(courseId)
        }
    }

    fun updateUiState(reducer: (TimetableUiState) -> TimetableUiState) {
        _uiState.value = reducer(_uiState.value)
    }

    fun updateUiState(state: TimetableUiState) {
        _uiState.value = state
    }

    data class PendingImport(val metadata: TableMetadata, val courses: List<Course>)
    var pendingImport by mutableStateOf<PendingImport?>(null); private set
    var importErrorMessage by mutableStateOf<String?>(null); private set

    fun handleImportedJson(jsonString: String) = viewModelScope.launch {
        try {
            importErrorMessage = null
            isImporting = true
            val (metadata, courses) = courseRepository.parseExternalJson(jsonString)
            pendingImport = PendingImport(metadata, courses)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse imported json", e)
            importErrorMessage = e.message ?: "解析失败"
        } finally {
            isImporting = false
        }
    }

    fun confirmImport(finalMetadata: TableMetadata) {
        val importData = pendingImport ?: return
        viewModelScope.launch {
            try {
                isImporting = true
                val newTableId = courseRepository.saveTable(finalMetadata)
                val coursesToSave = importData.courses.map { it.copy(tableId = newTableId) }
                courseRepository.saveCourses(coursesToSave)
                courseRepository.setCurrentTable(newTableId)
                _currentWeek.value = 1
                isInternalWeekUpdate = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save table", e)
                importErrorMessage = "保存失败: ${e.message}"
            } finally {
                isImporting = false
                pendingImport = null
                Logger.d(TAG, "Import sequence finished for table ID: ${currentTable.value?.id}")
            }
        }
    }

    fun cancelImport() {
        pendingImport = null
    }

}

class TimetableViewModelFactory(
    private val courseRepository: CourseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimetableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimetableViewModel(courseRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
