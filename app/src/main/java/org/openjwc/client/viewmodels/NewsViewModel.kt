package org.openjwc.client.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.net.models.UploadedNotice
import org.openjwc.client.net.models.NetClient
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.ReviewedNoticesData
import org.openjwc.client.net.news.fetchLabels
import org.openjwc.client.net.news.fetchNews
import org.openjwc.client.net.news.uploadNews
import org.openjwc.client.net.news.fetchReviewedNews

class NewsViewModel(
    private val repository: SettingsRepository
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

    var reviewedNoticesError = MutableStateFlow<String?>(null)
    fun getNewsState(label: String): List<FetchedNotice> = _newsCache[label] ?: emptyList()
    fun getError(label: String): String? = _errorMap[label]
    fun isEnd(label: String): Boolean = _isEndMap[label] ?: false

    fun loadLabels() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                val settings = repository.getSettingsSnapshot()
                val apiService = NetClient.getService(settings.host, settings.port, settings.useHttp, settings.proxy)

                val result = apiService.fetchLabels(
                    settings.authKey,
                    settings.uuidString,
                )

                when (result) {
                    is NetworkResult.Success -> {
                        labels.value = result.response.data.labels
                        labelError.value = null
                    }

                    is NetworkResult.Failure -> {
                        labelError.value = "加载错误(${result.code}): ${result.msg}"
                    }

                    is NetworkResult.Error -> {
                        labelError.value = result.msg
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "loadLabels Error", e)
                labelError.value = e.localizedMessage ?: "未知错误"
            } finally {
                isRefreshing.value = false
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

    private fun executeLoadNews(label: String, page: Int, size: Int, isRefresh: Boolean) {
        if (isRefresh) isRefreshing.value = true else isLoading.value = true
        _errorMap[label] = null

        viewModelScope.launch {
            try {
                val settings = repository.getSettingsSnapshot()
                val apiService = NetClient.getService(settings.host, settings.port, settings.useHttp, settings.proxy)

                val result = apiService.fetchNews(
                    settings.authKey,
                    settings.uuidString,
                    label,
                    page,
                    size
                )

                when (result) {
                    is NetworkResult.Success -> {
                        val newData = result.response.data.fetchedNotices
                        for (notice in newData) {
                            Log.d(tag, "notice: $notice")
                        }
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
                    }

                    is NetworkResult.Error -> {
                        _errorMap[label] = result.msg
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "executeLoad Error", e)
                _errorMap[label] = e.localizedMessage ?: "未知错误"
            } finally {
                isLoading.value = false
                isRefreshing.value = false
            }
        }
    }

    suspend fun uploadNews(uploadedNotice: UploadedNotice): Boolean {
        uploadError.value = null
        return try {
            val settings = repository.getSettingsSnapshot()
            val apiService = NetClient.getService(settings.host, settings.port, settings.useHttp, settings.proxy)

            val result = apiService.uploadNews(
                settings.authKey,
                settings.uuidString,
                uploadedNotice
            )

            when (result) {
                is NetworkResult.Success -> {
                    uploadError.value = null
                    true
                }

                is NetworkResult.Failure -> {
                    uploadError.value = "加载错误(${result.code}): ${result.msg}"
                    false
                }

                is NetworkResult.Error -> {
                    uploadError.value = result.msg
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "uploadNews Error", e)
            uploadError.value = e.localizedMessage ?: "未知错误"
            false
        }
    }

    fun clearUploadError() {
        uploadError.value = null
    }

    fun fetchReviewedNotices() {
        viewModelScope.launch {
            try {
                val settings = repository.getSettingsSnapshot()
                val apiService = NetClient.getService(settings.host, settings.port, settings.useHttp, settings.proxy)

                val result = apiService.fetchReviewedNews(
                    settings.authKey,
                    settings.uuidString,
                )
                when (result) {
                    is NetworkResult.Success -> {
                        reviewedNoticesData.value = result.response.data
                        reviewedNoticesError.value = null
                    }

                    is NetworkResult.Failure -> {
                        reviewedNoticesError.value = "加载错误(${result.code}): ${result.msg}"
                    }

                    is NetworkResult.Error -> {
                        reviewedNoticesError.value = result.msg
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "fetchReviewedNotices Error", e)
                reviewedNoticesError.value = e.localizedMessage
            }
        }
    }
}


class NewsViewModelFactory(
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}