package org.openjwc.client.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openjwc.client.data.models.DailyReportStatus
import org.openjwc.client.data.repository.DailyReportRepository
import org.openjwc.client.log.Logger
import java.time.LocalDate

data class DailyReportUiState(
    val loading: Boolean = true,
    /** 下拉刷新中（重读数据库，必要时顺带生成）。 */
    val refreshing: Boolean = false,
    /** 正在调用模型生成日报。 */
    val generating: Boolean = false,
    val dates: List<String> = emptyList(),
    val selectedDate: String? = null,
    val content: String? = null,
    val error: String? = null,
    /** 该日期生成失败（原因在 [error]），可点击重试。 */
    val failedDay: String? = null,
)

/** 本地日报：内容由 [DailyReportRepository] 调用用户自己的模型生成并落库。 */
class DailyReportViewModel(
    private val repository: DailyReportRepository,
) : ViewModel() {
    private val label = "DailyReportViewModel"
    private val _uiState = MutableStateFlow(DailyReportUiState())
    val uiState: StateFlow<DailyReportUiState> = _uiState.asStateFlow()

    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set

    init {
        viewModelScope.launch {
            repository.observeCompletedDays().collect { days ->
                _uiState.update { it.copy(dates = days) }
            }
        }
        refresh()
    }

    /** 重新载入：保留当前选中的日期，未选中时取最近一份。 */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            reload()
        }
    }

    /**
     * 下拉刷新：重读数据库；若目标日期还没有已完成日报，就顺手生成一次
     * （等同于手动重试，不会覆盖已完成的日报）。
     */
    fun pullRefresh() {
        if (_uiState.value.refreshing || _uiState.value.generating) return
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, error = null) }
            try {
                reload()
                val target = _uiState.value.selectedDate
                    ?: LocalDate.now().minusDays(1).toString()
                val needsGenerate = _uiState.value.content == null &&
                    repository.statusOf(target) != DailyReportStatus.RUNNING
                if (needsGenerate) {
                    Logger.i(label, "下拉刷新：$target 还没有日报，触发生成")
                    generateInternal(target)
                    reload()
                }
            } finally {
                _uiState.update { it.copy(refreshing = false) }
            }
        }
    }

    fun selectDate(date: String) {
        viewModelScope.launch {
            val report = runCatching { repository.getCompleted(date) }.getOrNull()
            _uiState.update {
                it.copy(
                    selectedDate = date,
                    content = report?.content,
                    error = if (report == null) NO_REPORT_MESSAGE else null
                )
            }
        }
    }

    /** 生成日报（默认昨天）。已完成的日期会直接返回。 */
    fun generate(day: String? = null) {
        if (_uiState.value.generating) return
        val target = day ?: _uiState.value.selectedDate
            ?: LocalDate.now().minusDays(1).toString()
        viewModelScope.launch {
            _uiState.update { it.copy(generating = true, error = null) }
            try {
                generateInternal(target)
            } finally {
                _uiState.update { it.copy(generating = false) }
            }
            reload()
        }
    }

    /** 真正执行生成，并把失败原因通过 Toast 抛出。 */
    private suspend fun generateInternal(day: String) {
        val result = repository.generate(day)
        result.exceptionOrNull()?.let { error ->
            Logger.e(label, "生成日报失败: ${error.message}", error)
            uiEvent.send(
                UiEvent.ShowToast(
                    UiText.DynamicString(error.localizedMessage ?: "日报生成失败")
                )
            )
        }
    }

    /** 重读当前选中日期（或最近一份）的内容；没有完成稿时看看是不是生成失败了。 */
    private suspend fun reload() {
        val selected = _uiState.value.selectedDate
        val target = selected ?: LocalDate.now().minusDays(1).toString()
        val report = runCatching {
            if (selected != null) repository.getCompleted(selected)
            else repository.latestCompleted(LocalDate.now().toString())
        }.getOrNull()

        if (report != null) {
            Logger.d(label, "reload selected=$selected report=${report.day}")
            _uiState.update {
                it.copy(
                    loading = false,
                    selectedDate = selected ?: report.day,
                    content = report.content,
                    error = null,
                    failedDay = null,
                )
            }
            return
        }

        val record = runCatching { repository.getRecord(target) }.getOrNull()
        val failed = record?.takeIf { it.status == DailyReportStatus.FAILED.value }
        Logger.d(label, "reload selected=$selected 无完成稿 target=$target failed=${failed?.day}")
        _uiState.update {
            it.copy(
                loading = false,
                selectedDate = selected ?: target,
                content = null,
                error = failed?.error ?: if (selected != null) NO_REPORT_MESSAGE else null,
                failedDay = failed?.day,
            )
        }
    }

    private companion object {
        const val NO_REPORT_MESSAGE = "该日期没有已完成的日报"
    }
}

class DailyReportViewModelFactory(
    private val repository: DailyReportRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DailyReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DailyReportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
