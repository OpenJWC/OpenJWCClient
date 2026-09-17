package org.openjwc.client.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openjwc.client.data.dao.NoticeDao
import org.openjwc.client.data.dao.SourceDao
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.models.SourceEntity
import org.openjwc.client.data.source.SourceRegistry
import org.openjwc.client.data.source.CrawlOutcome
import org.openjwc.client.data.source.SourceRunner
import org.openjwc.client.log.Logger
import org.openjwc.client.R

data class SourcesUiState(
    val running: Boolean = false,
)

class SourcesViewModel(
    private val registry: SourceRegistry,
    private val runner: SourceRunner,
    private val sourceDao: SourceDao,
    private val noticeDao: NoticeDao,
    private val settingsDataSource: SettingsDataSource,
) : ViewModel() {

    private val tag = "SourcesViewModel"

    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set

    /** 当前查看的数据源（数据源属性页）。 */
    private val _selectedSourceId = MutableStateFlow<String?>(null)

    val selectedSource: StateFlow<SourceEntity?> = combine(
        _selectedSourceId,
        sourceDao.observeAll(),
    ) { id, list -> id?.let { selected -> list.firstOrNull { it.id == selected } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 属性页里的脚本编辑内容。 */
    private val _script = MutableStateFlow<String?>(null)
    val script: StateFlow<String?> = _script.asStateFlow()

    fun selectSource(sourceId: String) {
        _selectedSourceId.value = sourceId
        _script.value = null
        viewModelScope.launch {
            val entity = sourceDao.getById(sourceId) ?: return@launch
            _script.value = registry.scriptText(entity)
            Logger.d(tag, "打开数据源属性页：${sourceId}（内置=${entity.isBuiltIn}）")
        }
    }

    /** 保存脚本：先静态校验（语法 + fetchNotices），再落盘；内置脚本只读，不允许保存。 */
    fun saveScript(text: String) = viewModelScope.launch {
        val entity = selectedSource.value ?: return@launch
        if (entity.isBuiltIn) {
            Logger.w(tag, "内置数据源 ${entity.id} 不允许修改脚本")
            uiEvent.send(UiEvent.ShowToast(UiText.StringResource(R.string.source_builtin_readonly)))
            return@launch
        }
        val scriptError = runner.validateScript(text)
        if (scriptError != null) {
            Logger.e(tag, "保存脚本失败：校验未通过 - $scriptError")
            uiEvent.send(
                UiEvent.ShowToast(
                    UiText.StringResource(R.string.source_script_invalid_reason, scriptError)
                )
            )
            return@launch
        }
        val updated = registry.saveScript(entity, text)
        if (updated == null) {
            Logger.e(tag, "保存脚本失败：缺少 @id 或 @id 与数据源不一致")
            uiEvent.send(
                UiEvent.ShowToast(UiText.StringResource(R.string.source_script_invalid))
            )
            return@launch
        }
        // 回写编辑器内容，让「未保存」状态复位
        _script.value = text
        uiEvent.send(UiEvent.ShowToast(UiText.StringResource(R.string.source_script_saved)))
    }

    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    val sources: StateFlow<List<SourceEntity>> = sourceDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 抓取进度（对话框用）。 */
    var crawlProgress = MutableStateFlow(CrawlProgress())
        private set

    fun dismissCrawlProgress() {
        crawlProgress.value = CrawlProgress()
    }

    /** 各数据源的本地语料条数（id -> 条数）。 */
    val counts: StateFlow<Map<String, Int>> = noticeDao.observeCountsBySource()
        .map { list -> list.mapNotNull { row -> row.sourceId?.let { it to row.noticeCount } }.toMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** 语料条数（「存储与缓存」）。 */
    val cacheCount: StateFlow<Int> = noticeDao.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 抓取回溯天数（脚本只抓最近这么多天）。 */
    val crawlDaysGap: StateFlow<Int> = settingsDataSource.userSettings
        .map { it.crawlDaysGap }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 200)

    init {
        viewModelScope.launch {
            runCatching { registry.syncBuiltIns() }
                .onFailure { Logger.e(tag, "同步内置脚本失败: ${it.message}", it) }
        }
    }

    fun setSubscribed(sourceId: String, subscribed: Boolean) = viewModelScope.launch {
        Logger.i(tag, "数据源 $sourceId 订阅状态 -> $subscribed")
        registry.setSubscribed(sourceId, subscribed)
    }

    fun setCrawlDaysGap(days: Int) = viewModelScope.launch {
        Logger.i(tag, "抓取回溯天数更新为 $days 天")
        settingsDataSource.save(SettingsDataSource.Keys.CRAWL_DAYS_GAP, days)
    }

    /** 清空本地资讯语料。 */
    fun clearCache() = viewModelScope.launch {
        runCatching { noticeDao.clearAll() }
            .onSuccess { Logger.i(tag, "已清空本地资讯语料") }
            .onFailure { Logger.e(tag, "清空资讯语料失败: ${it.message}", it) }
        uiEvent.send(UiEvent.ShowToast(UiText.StringResource(R.string.cache_cleared)))
    }

    fun run(source: SourceEntity) = viewModelScope.launch {
        runSource(source, notify = true)
    }

    fun delete(sourceId: String) = viewModelScope.launch {
        if (!registry.delete(sourceId)) {
            uiEvent.send(
                UiEvent.ShowToast(UiText.StringResource(R.string.source_delete_builtin_forbidden))
            )
            return@launch
        }
        if (_selectedSourceId.value == sourceId) _selectedSourceId.value = null
        uiEvent.send(UiEvent.ShowToast(UiText.StringResource(R.string.source_deleted)))
    }

    /** 导入脚本：读取 → 静态校验 → 注册。 */
    fun importFromUri(uri: Uri) = viewModelScope.launch {
        val text = registry.readScriptFromUri(uri)
        if (text == null) {
            Logger.e(tag, "导入失败：无法读取所选文件")
            uiEvent.send(UiEvent.ShowToast(UiText.StringResource(R.string.source_import_read_failed)))
            return@launch
        }
        val scriptError = runner.validateScript(text)
        if (scriptError != null) {
            Logger.e(tag, "导入失败：脚本校验未通过 - $scriptError")
            uiEvent.send(
                UiEvent.ShowToast(
                    UiText.StringResource(R.string.source_script_invalid_reason, scriptError)
                )
            )
            return@launch
        }
        val entity = registry.register(text)
        if (entity == null) {
            Logger.e(tag, "导入失败：脚本缺少 @id")
            uiEvent.send(
                UiEvent.ShowToast(
                    UiText.StringResource(R.string.source_script_invalid_reason, "缺少 @id")
                )
            )
            return@launch
        }
        Logger.i(tag, "已导入数据源 ${entity.id}（v${entity.version}）")
        uiEvent.send(
            UiEvent.ShowToast(UiText.StringResource(R.string.source_import_success, entity.name))
        )
    }

    private var runAllJob: Job? = null

    fun runAllSubscribed() {
        if (_uiState.value.running) return
        runAllJob = viewModelScope.launch {
            _uiState.update { it.copy(running = true) }
            var succeeded = 0
            val subscribed = sourceDao.getSubscribed()
            crawlProgress.value = CrawlProgress(running = true, total = subscribed.size)
            try {
                for ((index, source) in subscribed.withIndex()) {
                    crawlProgress.update {
                        it.copy(
                            finished = index,
                            currentSourceName = source.name,
                            currentSourceFraction = 0f,
                            logs = it.logs + "▶ ${source.name}",
                        )
                    }
                    if (runSource(source, notify = false)) succeeded++
                    crawlProgress.update { it.copy(finished = index + 1, currentSourceFraction = 0f) }
                }
                uiEvent.send(
                    UiEvent.ShowToast(
                        UiText.StringResource(R.string.source_run_all_done, succeeded)
                    )
                )
            } finally {
                _uiState.update { it.copy(running = false) }
                crawlProgress.update { it.copy(running = false, currentSourceName = null) }
            }
        }
    }

    /** 取消「全部抓取」。 */
    fun cancelRunAll() {
        runAllJob?.cancel()
    }

    private suspend fun runSource(source: SourceEntity, notify: Boolean): Boolean {
        val crawlDaysGap = settingsDataSource.userSettings.first().crawlDaysGap
        val outcome = runner.crawl(
            source,
            crawlDaysGap,
            onLog = { line ->
                crawlProgress.update { state ->
                    state.copy(logs = (state.logs + line).takeLast(MAX_CRAWL_LOGS))
                }
            },
            onProgress = { fraction, _ ->
                crawlProgress.update { state -> state.copy(currentSourceFraction = fraction.toFloat()) }
            },
        )
        crawlProgress.update {
            it.copy(results = it.results + CrawlResult(source.name, summarize(outcome)))
        }
        // 手动抓取视为已读：只推进通知水位，不弹系统通知
        runner.settleNotifications(outcome, notify = false)

        if (notify) {
            val message = if (outcome.success) {
                UiText.StringResource(R.string.source_run_result, source.name, outcome.notices.size)
            } else {
                UiText.StringResource(R.string.source_run_failed, source.name, outcome.error.orEmpty())
            }
            uiEvent.send(UiEvent.ShowToast(message))
        }
        return outcome.success
    }
    private fun summarize(outcome: CrawlOutcome): String = buildString {
        append("新增 ").append(outcome.newNotices.size).append(" 条")
        if (outcome.noContent > 0) append("，无正文 ").append(outcome.noContent).append(" 条")
        if (outcome.failed > 0) append("，失败 ").append(outcome.failed).append(" 条")
        outcome.error?.let { append("，失败：").append(it) }
    }

    private companion object {
        const val MAX_CRAWL_LOGS = 200
    }
}

class SourcesViewModelFactory(
    private val registry: SourceRegistry,
    private val runner: SourceRunner,
    private val sourceDao: SourceDao,
    private val noticeDao: NoticeDao,
    private val settingsDataSource: SettingsDataSource,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SourcesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SourcesViewModel(registry, runner, sourceDao, noticeDao, settingsDataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
