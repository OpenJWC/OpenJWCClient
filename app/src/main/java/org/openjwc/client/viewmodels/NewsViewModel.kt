package org.openjwc.client.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openjwc.client.R
import org.openjwc.client.data.datastore.UserSettings
import org.openjwc.client.data.models.SourceEntity
import org.openjwc.client.data.models.toFetchedNotice
import org.openjwc.client.data.repository.NewsRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.source.CrawlOutcome
import org.openjwc.client.data.source.SourceRunner
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.FetchedNotice

/** 一次「抓取已订阅数据源」的进度，用于进度对话框。 */
data class CrawlProgress(
    val running: Boolean = false,
    val total: Int = 0,
    val finished: Int = 0,
    /** 当前正在抓的源内部的完成比例 0..1（脚本上报，用于更细粒度进度条）。 */
    val currentSourceFraction: Float = 0f,
    val currentSourceName: String? = null,
    val results: List<CrawlResult> = emptyList(),
    /** 脚本实时日志（最近的若干行）。 */
    val logs: List<String> = emptyList(),
)

data class CrawlResult(val sourceName: String, val summary: String)

data class PagingState(
    val items: List<FetchedNotice> = emptyList(),
    val currentPage: Int = 1,
    val isEnd: Boolean = false,
    val error: String? = null
)

private const val PAGE_SIZE = 20

class NewsViewModel(
    repository: SettingsRepository,
    private val newsRepository: NewsRepository,
    private val sourceRunner: SourceRunner,
) : ViewModel() {
    private val tag = "NewsViewModel"

    private val _pagingStates = mutableStateMapOf<String, PagingState>()

    init {
        // 后台抓取完成后语料会变化：自动重读已加载过的栏目，避免界面停留在旧数据
        viewModelScope.launch {
            newsRepository.observeNoticeCount()
                .drop(1)
                .distinctUntilChanged()
                .collect { refreshLoadedLabels() }
        }
    }

    /** 重读已加载栏目（保持已翻页数），不改动刷新指示器。 */
    private fun refreshLoadedLabels() {
        val labels = _pagingStates.keys.toList()
        labels.forEach { label ->
            viewModelScope.launch {
                val state = _pagingStates[label] ?: return@launch
                val limit = state.currentPage * PAGE_SIZE
                val items = runCatching {
                    newsRepository.getLocalNews(label, sourceFilter.value, limit, 0)
                }.getOrNull() ?: return@launch
                _pagingStates[label] = state.copy(items = items)
            }
        }
    }

    val freshDays: StateFlow<Int?> = repository.userSettings
        .map { it.freshDays }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings().freshDays
        )

    val crawlDaysGap: StateFlow<Int> = repository.userSettings
        .map { it.crawlDaysGap }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings().crawlDaysGap
        )

    val favoriteNews = newsRepository.observeFavorites()
        .distinctUntilChanged().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val newsCacheCount: StateFlow<Int> = newsRepository.observeNoticeCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    var currentNewsToDisplay = MutableStateFlow<FetchedNotice?>(null)
        private set

    /** 正文里被点开的图片地址（图片查看器路由的数据）。 */
    /**
     * 详情页正文是否已可渲染（打开动画播完）。
     *
     * 放在 ViewModel 而不是 remember：查看器打开时详情页 entry 会被 dispose，
     * 返回时整页重新组合；若用 remember 就会在返回时重新「空白 → 渲染」，
     * 导致正文布局抖动、多张图片一起位移。
     */
    var detailContentReady by mutableStateOf(false)
        private set

    private var preparedNoticeId: String? = null

    /**
     * 详情页滚动位置（按资讯 id）。
     *
     * 打开图片查看器时详情页 entry 会被 dispose，返回时整页重建、滚动位置会丢，
     * 导致「返回后回到开头」以及共享元素目标位置错乱（图片乱飞）。
     */
    private val detailScrollOffsets = mutableMapOf<String, Int>()

    fun saveDetailScroll(noticeId: String, offset: Int) {
        detailScrollOffsets[noticeId] = offset
    }

    fun detailScrollOffset(noticeId: String): Int = detailScrollOffsets[noticeId] ?: 0

    /** 打开某篇资讯时调用：等共享元素放大动画播完再渲染正文。 */
    fun prepareDetailContent(noticeId: String) {
        if (preparedNoticeId == noticeId && detailContentReady) return
        preparedNoticeId = noticeId
        detailContentReady = false
        viewModelScope.launch {
            delay(CONTENT_READY_DELAY_MILLIS)
            detailContentReady = true
        }
    }

    var viewerImageUrl = MutableStateFlow<String?>(null)
        private set

    var labels = MutableStateFlow<List<String>>(emptyList())
        private set

    var labelError = MutableStateFlow<String?>(null)
        private set

    var isLoading = MutableStateFlow(false)
        private set

    var isRefreshing = MutableStateFlow(false)
        private set

    /*var needsAuth = MutableStateFlow(false)
        private set*/
    /** 已订阅数据源，供资讯流的数据源筛选使用。 */
    val sources: StateFlow<List<SourceEntity>> = newsRepository.observeSubscribedSources()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** 当前数据源筛选：null = 全部数据源。 */
    var sourceFilter = MutableStateFlow<String?>(null)
        private set

    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set
    fun getNewsState(label: String): List<FetchedNotice> = _pagingStates[label]?.items ?: emptyList()
    fun getError(label: String): String? = _pagingStates[label]?.error
    fun isEnd(label: String): Boolean = _pagingStates[label]?.isEnd ?: false

    /** 附件选择 sheet：按数据源（null=全部）取该范围内的栏目。 */
    suspend fun attachmentLabels(sourceId: String?): List<String> =
        newsRepository.getLocalLabels(sourceId)

    /** 附件选择 sheet：按栏目 + 数据源（null=全部）取资讯。 */
    suspend fun attachmentNotices(
        label: String,
        sourceId: String?,
        limit: Int = 60,
    ): List<FetchedNotice> = newsRepository.getLocalNews(label, sourceId, limit, 0)

    /** 按 id 取一条资讯（工具卡片点击进入详情用）。 */
    suspend fun noticeById(id: String): FetchedNotice? =
        newsRepository.findNotice(id)?.toFetchedNotice()

    /** 切换数据源筛选：重置分页并重新加载栏目。 */
    fun setSourceFilter(sourceId: String?) {
        if (sourceFilter.value == sourceId) return
        sourceFilter.value = sourceId
        labels.value = emptyList()
        _pagingStates.clear()
        loadLabels()
    }

    fun loadLabels() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                labels.value = newsRepository.getLocalLabels(sourceFilter.value)
                labelError.value = null
            } catch (e: Exception) {
                Logger.e(tag, "loadLabels Error", e)
                labelError.value = e.localizedMessage ?: "未知错误"
            } finally {
                isRefreshing.value = false
            }
        }
    }

    /** 正在抓取中：重复下拉直接忽略，避免并发抓取同一批数据源。 */
    private var isCrawling = false

    /** 正在进行的抓取任务（下拉刷新所在协程），用于取消。 */
    private var crawlJob: Job? = null

    /** 取消正在进行的抓取。 */
    fun cancelCrawl() {
        crawlJob?.cancel()
    }

    /** 抓取进度（对话框用）。 */
    var crawlProgress = MutableStateFlow(CrawlProgress())
        private set

    fun dismissCrawlProgress() {
        crawlProgress.value = CrawlProgress()
    }

    /**
     * 下拉刷新：直接抓取当前筛选范围内的已订阅数据源（含翻页与正文补抓，可能较久，
     * 期间下拉刷新的转圈会一直转）。交互抓取视为已读，只推进通知水位。
     */
    private suspend fun crawlSubscribedSources() {
        if (isCrawling) {
            Logger.d(tag, "已有抓取在进行，忽略本次下拉刷新")
            return
        }
        isCrawling = true
        try {
            val sources = newsRepository.observeSubscribedSources().first()
            val picked = sourceFilter.value?.let { id -> sources.filter { it.id == id } } ?: sources
            val crawlDaysGap = crawlDaysGap.value
            Logger.i(tag, "下拉刷新：抓取 ${picked.size} 个数据源，回溯 $crawlDaysGap 天")
            crawlProgress.value = CrawlProgress(running = true, total = picked.size)
            for ((index, source) in picked.withIndex()) {
                crawlProgress.update {
                    it.copy(
                        finished = index,
                        currentSourceName = source.name,
                        currentSourceFraction = 0f,
                        logs = it.logs + "▶ ${source.name}",
                    )
                }
                val outcome = sourceRunner.crawl(
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
                sourceRunner.settleNotifications(outcome, notify = false)
                Logger.d(
                    tag,
                    "数据源 ${source.id} 抓取 ${outcome.notices.size} 条（新增 ${outcome.newNotices.size}）" +
                        outcome.error?.let { " 失败：$it" }.orEmpty(),
                )
                crawlProgress.update {
                    it.copy(
                        finished = index + 1,
                        currentSourceFraction = 0f,
                        results = it.results + CrawlResult(source.name, summarizeCrawl(outcome)),
                    )
                }
            }
        } finally {
            isCrawling = false
            crawlProgress.update { it.copy(running = false, currentSourceName = null) }
        }
    }

    private companion object {
        const val MAX_CRAWL_LOGS = 200
        const val TAG = "NewsViewModel"

        /** 打开动画时长（与共享元素转场一致）。 */
        const val CONTENT_READY_DELAY_MILLIS = 240L
    }

    private fun summarizeCrawl(outcome: CrawlOutcome): String = buildString {
        append("新增 ").append(outcome.newNotices.size).append(" 条")
        if (outcome.noContent > 0) append("，无正文 ").append(outcome.noContent).append(" 条")
        if (outcome.failed > 0) append("，失败 ").append(outcome.failed).append(" 条")
        outcome.error?.let { append("，失败：").append(it) }
    }

    private fun executeLoadNews(label: String, page: Int, size: Int, isRefresh: Boolean) {
        if (isRefresh) isRefreshing.value = true else isLoading.value = true
        _pagingStates[label] = (_pagingStates[label] ?: PagingState()).copy(error = null)

        viewModelScope.launch {
            try {
                if (isRefresh) {
                    crawlJob = coroutineContext[Job]
                    try {
                        crawlSubscribedSources()
                    } finally {
                        crawlJob = null
                    }
                }

                val offset = (page - 1) * size
                val newData = newsRepository.getLocalNews(
                    label = label,
                    sourceId = sourceFilter.value,
                    limit = size,
                    offset = offset,
                )
                val isEnd = newData.size < size

                if (page == 1) {
                    _pagingStates[label] = PagingState(
                        items = newData,
                        currentPage = 1,
                        isEnd = isEnd
                    )
                } else {
                    val current = _pagingStates[label] ?: PagingState()
                    _pagingStates[label] = current.copy(
                        items = (current.items + newData).distinctBy { it.id },
                        currentPage = page,
                        isEnd = isEnd
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(tag, "executeLoad Error", e)
                _pagingStates[label] = (_pagingStates[label] ?: PagingState()).copy(
                    error = e.localizedMessage ?: "未知错误"
                )
            } finally {
                isLoading.value = false
                isRefreshing.value = false
            }
        }
    }

    fun loadCategory(label: String, isRefresh: Boolean = false) {
        if (!isRefresh && _pagingStates.containsKey(label)) return
        executeLoadNews(label, page = 1, size = PAGE_SIZE, isRefresh = isRefresh)
    }

    fun loadNextPage(label: String) {
        if (isLoading.value || isRefreshing.value || isEnd(label)) return
        val nextPage = (_pagingStates[label]?.currentPage ?: 1) + 1
        executeLoadNews(label, page = nextPage, size = PAGE_SIZE, isRefresh = false)
    }

    fun clearNewsCache() {
        viewModelScope.launch {
            runCatching { newsRepository.clearNotices() }
            uiEvent.send(UiEvent.ShowToast(UiText.StringResource(R.string.cache_cleared)))
        }
    }

    fun deleteFavorite(noticeId: String) {
        viewModelScope.launch {
            newsRepository.setFavorite(noticeId, false)
        }
    }

    fun deleteAllFavorites() {
        viewModelScope.launch {
            newsRepository.clearFavorites()
        }
    }

    fun insertFavorite(notice: FetchedNotice) {
        viewModelScope.launch {
            newsRepository.setFavorite(notice.id, true)
        }
    }

    fun setCurrentNewsToDisplay(fetchedNotice: FetchedNotice?) {
        currentNewsToDisplay.value = fetchedNotice
    }

    fun openImageViewer(url: String) {
        Logger.d(TAG, "openImageViewer ${url.takeLast(28)}")
        viewerImageUrl.value = url
    }

    /**
     * 查看器 entry 真正销毁时才清空。
     *
     * 不能在点返回时立刻清空：返回转场期间详情页那张图片必须仍然是共享元素，
     * 否则图片无法缩回正文原位。
     */
    fun clearViewerImage() {
        Logger.d(TAG, "clearViewerImage")
        viewerImageUrl.value = null
    }

}


class NewsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val newsRepository: NewsRepository,
    private val sourceRunner: SourceRunner,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(settingsRepository, newsRepository, sourceRunner) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}