package org.openjwc.client.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.R
import org.openjwc.client.data.datastore.UserSettings
import org.openjwc.client.data.models.toFetchedNotice
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.data.repository.NewsRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.ReviewedNoticesData
import org.openjwc.client.net.models.UploadedNotice

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
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val tag = "NewsViewModel"

    private val _pagingStates = mutableStateMapOf<String, PagingState>()

    init {
        // 迁移遗留收藏（host='' 分区）归属到当前数据源（幂等）
        viewModelScope.launch {
            runCatching { newsRepository.adoptLegacyFavorites() }
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

    val favoriteNews = newsRepository.allFavorites()
        .map { favorites ->
            favorites.map {
                it.toFetchedNotice()
            }
        }
        .distinctUntilChanged().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val newsCacheCount: StateFlow<Int> = newsRepository.observeNewsCacheCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    var currentNewsToDisplay = MutableStateFlow<FetchedNotice?>(null)
        private set

    var labels = MutableStateFlow<List<String>>(emptyList())
        private set

    var uploadError = MutableStateFlow<String?>(null)
        private set

    var labelError = MutableStateFlow<String?>(null)
        private set

    var isLoading = MutableStateFlow(false)
        private set

    var isRefreshing = MutableStateFlow(false)
        private set

    var reviewedNoticesData = MutableStateFlow<ReviewedNoticesData?>(null)
        private set

    /*var needsAuth = MutableStateFlow(false)
        private set*/
    val needsAuth = authRepository.authSession.map { it.isLoggedIn }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    var navEvent = Channel<NavEvent>(Channel.BUFFERED)
        private set

    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set
    var reviewedNoticesError = MutableStateFlow<String?>(null)
    fun getNewsState(label: String): List<FetchedNotice> = _pagingStates[label]?.items ?: emptyList()
    fun getError(label: String): String? = _pagingStates[label]?.error
    fun isEnd(label: String): Boolean = _pagingStates[label]?.isEnd ?: false

    fun loadLabels() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                when (val result = newsRepository.getLabels()) {
                    is NetworkResult.Success -> {
                        labels.value = result.response.data.labels
                        labelError.value = null
                        runCatching { newsRepository.saveLabelsCache(result.response.data.labels) }
                    }

                    is NetworkResult.Failure -> {
                        // 离线回退：有标签缓存则静默使用，不报错
                        val cached = runCatching { newsRepository.getCachedLabels() }.getOrNull()
                        if (cached != null) {
                            labels.value = cached
                        } else {
                            labelError.value = "加载错误(${result.code}): ${result.msg}"
                        }
                    }

                    is NetworkResult.Error -> {
                        val cached = runCatching { newsRepository.getCachedLabels() }.getOrNull()
                        if (cached != null) {
                            labels.value = cached
                        } else {
                            labelError.value = result.msg
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e(tag, "loadLabels Error", e)
                labelError.value = e.localizedMessage ?: "未知错误"
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private fun executeLoadNews(label: String, page: Int, size: Int, isRefresh: Boolean) {
        if (isRefresh) isRefreshing.value = true else isLoading.value = true
        _pagingStates[label] = (_pagingStates[label] ?: PagingState()).copy(error = null)

        viewModelScope.launch {
            try {
                val result = newsRepository.getNews(label, page, size)

                when (result) {
                    is NetworkResult.Success -> {
                        val newData = result.response.data.fetchedNotices
                        val isEnd = newData.size < size

                        if (isRefresh || page == 1) {
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

                        // 写入离线缓存（失败不影响 UI）
                        runCatching { newsRepository.refreshNewsCacheFromUi(label, newData) }
                    }

                    is NetworkResult.Failure -> {
                        _pagingStates[label] = (_pagingStates[label] ?: PagingState()).copy(
                            error = "加载错误(${result.code}): ${result.msg}"
                        )
                    }

                    is NetworkResult.Error -> {
                        _pagingStates[label] = (_pagingStates[label] ?: PagingState()).copy(
                            error = result.msg
                        )
                    }
                }
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

    fun uploadNews(uploadedNotice: UploadedNotice) {
        viewModelScope.launch {
            uploadError.value = null
            try {
                val result = newsRepository.uploadNews(uploadedNotice)
                when (result) {
                    is NetworkResult.Success -> {
                        navEvent.send(NavEvent.ToBack())
                        uiEvent.send(UiEvent.ShowToast(UiText.StringResource(R.string.upload_success)))
                    }

                    is NetworkResult.Failure -> {
                        uploadError.value = "加载错误(${result.code}): ${result.msg}"
                    }

                    is NetworkResult.Error -> {
                        uploadError.value = result.msg

                    }
                }
            } catch (e: Exception) {
                Logger.e(tag, "uploadNews Error", e)
                uploadError.value = e.localizedMessage ?: "未知错误"
            }
        }
    }

    fun fetchReviewedNotices() {
        viewModelScope.launch {
            try {
                val result = newsRepository.getReviewedNews()
                when (result) {
                    is NetworkResult.Success -> {
                        reviewedNoticesData.value = result.response.data
                        reviewedNoticesError.value = null
                    }

                    is NetworkResult.Failure -> {
                        reviewedNoticesError.value =
                            "加载错误(${result.code}): ${result.msg}"
                    }

                    is NetworkResult.Error -> reviewedNoticesError.value = result.msg
                }
            } catch (e: Exception) {
                Logger.e(tag, "fetchReviewedNotices Error", e)
                reviewedNoticesError.value = e.localizedMessage
            }
        }
    }

    fun loadCategory(label: String, isRefresh: Boolean = false) {
        if (!isRefresh && _pagingStates.containsKey(label)) return
        if (!_pagingStates.containsKey(label)) hydrateFromCache(label)
        executeLoadNews(label, page = 1, size = PAGE_SIZE, isRefresh = isRefresh)
    }

    /** 缓存打底：进标签先展示本地缓存（仅当内存里还没有数据时），随后网络刷新覆盖。 */
    private fun hydrateFromCache(label: String) {
        viewModelScope.launch {
            val cached = runCatching { newsRepository.getCachedNews(label) }.getOrDefault(emptyList())
            if (cached.isEmpty()) return@launch
            val current = _pagingStates[label]
            if (current == null || current.items.isEmpty()) {
                _pagingStates[label] = PagingState(items = cached, currentPage = 1, isEnd = false)
            }
        }
    }

    fun loadNextPage(label: String) {
        if (isLoading.value || isRefreshing.value || isEnd(label)) return
        val nextPage = (_pagingStates[label]?.currentPage ?: 1) + 1
        executeLoadNews(label, page = nextPage, size = PAGE_SIZE, isRefresh = false)
    }

    fun clearNewsCache() {
        viewModelScope.launch {
            runCatching { newsRepository.clearNewsCache() }
            uiEvent.send(UiEvent.ShowToast(UiText.StringResource(R.string.cache_cleared)))
        }
    }

    fun clearUploadError() {
        uploadError.value = null
    }

    fun deleteFavorite(noticeId: String) {
        viewModelScope.launch {
            newsRepository.deleteFavoriteNews(noticeId)
        }
    }

    fun deleteAllFavorites() {
        viewModelScope.launch {
            newsRepository.deleteAllFavorites()
        }
    }

    fun insertFavorite(notice: FetchedNotice) {
        viewModelScope.launch {
            newsRepository.insertFavoriteNews(notice)
        }
    }

    fun setCurrentNewsToDisplay(fetchedNotice: FetchedNotice?) {
        currentNewsToDisplay.value = fetchedNotice
    }
}


class NewsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val newsRepository: NewsRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(settingsRepository, newsRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}