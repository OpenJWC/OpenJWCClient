package org.openjwc.client.viewmodels

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

    // --- 1. UI 弹窗与交互状态 ---
    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    // --- 2. 数据库同步数据流 ---
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

    // 计算当前课表中已被使用的最大节次
    val currentMaxPeriodInUse = currentTableCourses.map { courses ->
        courses.maxOfOrNull { it.startPeriod + it.duration - 1 } ?: 0
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // --- 3. 周次管理 ---
    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    // 内部状态标记，用于 Pager 滚动同步
    var isInternalWeekUpdate by mutableStateOf(false)
        private set

    // --- 4. 实时时间线/节次高亮 ---
    private val _activePeriodIndex = MutableStateFlow(-1)
    val activePeriodIndex = _activePeriodIndex.asStateFlow()

    // --- 5. 显示偏好设置 ---
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
        // 自动计算并同步当前真实周次
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

    // --- 业务方法 ---

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

    // --- UI 状态转换方法 ---

    fun updateUiState(reducer: (TimetableUiState) -> TimetableUiState) {
        _uiState.value = reducer(_uiState.value)
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
