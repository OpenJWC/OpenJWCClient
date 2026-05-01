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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.TableMetadata
import org.openjwc.client.data.repository.CourseRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.log.Logger
import org.openjwc.client.ui.timetable.view.state.TimetableUiState
import java.time.LocalTime

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

    val allTables = courseRepository.allTables

    val currentTable = courseRepository.currentTable.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val currentTableCourses = courseRepository.currentCourses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentMaxPeriodInUse = currentTableCourses.map { courses ->
        courses.maxOfOrNull { it.startPeriod + it.duration - 1 } ?: 0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    // 内部状态标记，用于 Pager 滚动同步
    var isInternalWeekUpdate by mutableStateOf(false)
        private set

    private val _activePeriodIndex = MutableStateFlow(-1)
    val activePeriodIndex = _activePeriodIndex.asStateFlow()

    // TODO
    val displayPrefs: StateFlow<TimetableDisplayPrefs> = settingsRepository.userSettings
        .map { settings ->
            TimetableDisplayPrefs(
                showTimeline = true,
                showDate = true,
                showPeriodTime = true,
                showNonCurrentWeek = true
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = TimetableDisplayPrefs()
        )

    var isImporting by mutableStateOf(false)
        private set

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            currentTable.collect { table ->
                table?.semesterConfig?.let { config ->
                    val calculated = config.calculateCurrentWeek() ?: 1
                    _currentWeek.value = calculated
                    isInternalWeekUpdate = true
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

    fun nextWeek() {
        val maxWeeks = currentTable.value?.semesterConfig?.weeks ?: 1
        if (_currentWeek.value < maxWeeks) {
            _currentWeek.value++
            isInternalWeekUpdate = false
        }
    }

    fun prevWeek() {
        if (_currentWeek.value > 1) {
            _currentWeek.value--
            isInternalWeekUpdate = false
        }
    }

    fun switchTable(tableId: Long) {
        viewModelScope.launch {
            courseRepository.setCurrentTable(tableId)
        }
    }

    fun createTable(table: TableMetadata) {
        viewModelScope.launch {
            courseRepository.saveTable(table)
        }
    }

    fun updateTable(table: TableMetadata) {
        viewModelScope.launch {
            courseRepository.updateTable(table)
        }
    }

    fun deleteTable(tableId: Long) {
        viewModelScope.launch {
            courseRepository.deleteTable(tableId)
        }
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
                // 1. 保存课表并获取数据库生成的真实 ID
                val newTableId = courseRepository.saveTable(finalMetadata)

                // 2. 将所有课程关联到这个真实的 tableId
                val coursesToSave = importData.courses.map { course ->
                    course.copy(tableId = newTableId)
                }

                // 3. 批量保存关联了正确 ID 的课程
                courseRepository.saveCourses(coursesToSave)

                // 4. 切换当前课表为新生成的 ID
                courseRepository.setCurrentTable(newTableId)

                // 5. 更新 UI 状态
                _currentWeek.value = 1
                isInternalWeekUpdate = true
                pendingImport = null

            } catch (e: Exception) {
                Log.e(TAG, "Failed to save table", e)
                importErrorMessage = "保存失败: ${e.message}"
            }

            finally {
                Logger.d(TAG, "当前已有的课表：${courseRepository.currentTable}")
            }
        }
    }

    fun cancelImport() {
        pendingImport = null
    }

    fun clearImportError() {
        importErrorMessage = null
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
