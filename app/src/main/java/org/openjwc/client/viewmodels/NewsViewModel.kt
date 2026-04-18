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
import org.openjwc.client.data.datastore.UserSettings
import org.openjwc.client.data.models.NoticeEntity
import org.openjwc.client.data.models.toFetchedNotice
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.data.repository.NewsRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.ReviewedNoticesData
import org.openjwc.client.net.models.UploadedNotice

class NewsViewModel(
    repository: SettingsRepository,
    private val newsRepository: NewsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val tag = "NewsViewModel"

    private val _newsCache = mutableStateMapOf<String, List<FetchedNotice>>()
    private val _pageMap = mutableMapOf<String, Int>()
    private val _isEndMap = mutableStateMapOf<String, Boolean>()
    private val _errorMap = mutableStateMapOf<String, String?>()

    val freshDays: StateFlow<Int?> = repository.userSettings
        .map { it.freshDays }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings().freshDays
        )

    val favoriteNews = newsRepository.allFavorites
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
    fun getNewsState(label: String): List<FetchedNotice> = _newsCache[label] ?: emptyList()
    fun getError(label: String): String? = _errorMap[label]
    fun isEnd(label: String): Boolean = _isEndMap[label] ?: false

    fun loadLabels() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                when (val result = newsRepository.getLabels()) {
                    is NetworkResult.Success -> {
                        labels.value = result.response.data.labels
                        labelError.value = null
                    }

                    is NetworkResult.Failure -> {
                        labelError.value = "加载错误(${result.code}): ${result.msg}"
                        if (result.code == 401) {
                            authRepository.clearSession()
                        }
                    }

                    is NetworkResult.Error -> {
                        labelError.value = result.msg
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
        _errorMap[label] = null

        viewModelScope.launch {
            try {
                val result = newsRepository.getNews(label, page, size)

                when (result) {
                    is NetworkResult.Success -> {
                        val newData = result.response.data.fetchedNotices
                        _isEndMap[label] = newData.size < size

                        if (isRefresh || page == 1) {
                            _newsCache[label] = newData
                            _pageMap[label] = 1
                        } else {
                            val currentList = _newsCache[label] ?: emptyList()
                            _newsCache[label] = (currentList + newData).distinctBy { it.id }
                            _pageMap[label] = page
                        }
                    }

                    is NetworkResult.Failure -> {
                        _errorMap[label] = "加载错误(${result.code}): ${result.msg}"
                        if (result.code == 401) {
                            authRepository.clearSession()
                        }
                    }

                    is NetworkResult.Error -> _errorMap[label] = result.msg
                }
            } catch (e: Exception) {
                Logger.e(tag, "executeLoad Error", e)
                _errorMap[label] = e.localizedMessage ?: "未知错误"
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
                        uiEvent.send(UiEvent.ShowToast("上传成功"))
                    }

                    is NetworkResult.Failure -> {
                        uploadError.value = "加载错误(${result.code}): ${result.msg}"
                        if (result.code == 401) authRepository.clearSession()
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
                        if (result.code == 401) authRepository.clearSession()
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
        if (!isRefresh && _newsCache.containsKey(label)) return
        executeLoadNews(label, page = 1, size = 20, isRefresh = isRefresh)
    }

    fun loadNextPage(label: String) {
        if (isLoading.value || isRefreshing.value || isEnd(label)) return
        val nextPage = (_pageMap[label] ?: 1) + 1
        executeLoadNews(label, page = nextPage, size = 20, isRefresh = false)
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

    fun insertFavorite(notice: NoticeEntity) {
        viewModelScope.launch {
            newsRepository.insertFavoriteNews(notice)
        }
    }

    fun insertFavorite(notices: List<NoticeEntity>) {
        viewModelScope.launch {
            newsRepository.insertFavoriteNews(notices)
        }
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