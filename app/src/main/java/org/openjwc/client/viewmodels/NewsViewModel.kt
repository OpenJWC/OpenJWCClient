
package org.openjwc.client.viewmodels
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.net.models.FetchLabelsNetworkResult
import org.openjwc.client.net.models.FetchNewsNetworkResult
import org.openjwc.client.net.models.NetClient
import org.openjwc.client.net.models.Notice
import org.openjwc.client.net.news.fetchLabels
import org.openjwc.client.net.news.fetchNews

class NewsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {
    private val tag = "NewsViewModel"

    private val _newsCache = mutableStateMapOf<String, List<Notice>>()
    private val _pageMap = mutableMapOf<String, Int>()
    private val _isEndMap = mutableStateMapOf<String, Boolean>()
    private val _errorMap = mutableStateMapOf<String, String?>()
    var labels = MutableStateFlow<List<String>>(emptyList())
        private set

    var labelError = MutableStateFlow<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    fun getNewsState(label: String): List<Notice> = _newsCache[label] ?: emptyList()
    fun getError(label: String): String? = _errorMap[label]
    fun isEnd(label: String): Boolean = _isEndMap[label] ?: false

    fun loadLabels() {
        isRefreshing = true
        viewModelScope.launch {
            try {
                val settings = repository.getSettingsSnapshot() ?: UserSettings()
                val apiService = NetClient.getService(settings.host, settings.port)

                val result = apiService.fetchLabels(
                    settings.authKey,
                    settings.uuidString,
                )

                when (result) {
                    is FetchLabelsNetworkResult.Success -> {
                        labels.value = result.response.data.labels
                        labelError.value = null
                    }
                    is FetchLabelsNetworkResult.Failure -> {
                        labelError.value = "加载错误(${result.code}): ${result.msg}"
                    }
                    is FetchLabelsNetworkResult.Error -> {
                        labelError.value = result.msg
                    }
                }
            } catch (e : Exception) {
                Log.e(tag, "loadLabels Error", e)
                labelError.value = e.localizedMessage ?: "未知错误"
            }
        }
        isRefreshing = false
    }

    fun loadCategory(label: String, isRefresh: Boolean = false) {
        if (!isRefresh && _newsCache.containsKey(label)) return
        executeLoadNews(label, page = 1, size = 20, isRefresh = isRefresh)
    }

    fun loadNextPage(label: String) {
        if (isLoading || isRefreshing || isEnd(label)) return
        val nextPage = (_pageMap[label] ?: 1) + 1
        executeLoadNews(label, page = nextPage, size = 20, isRefresh = false)
    }

    private fun executeLoadNews(label: String, page: Int, size: Int, isRefresh: Boolean) {
        if (isRefresh) isRefreshing = true else isLoading = true
        _errorMap[label] = null

        viewModelScope.launch {
            try {
                val settings = repository.getSettingsSnapshot() ?: UserSettings()
                val apiService = NetClient.getService(settings.host, settings.port)

                val result = apiService.fetchNews(
                    settings.authKey,
                    settings.uuidString,
                    label,
                    page,
                    size
                )

                when (result) {
                    is FetchNewsNetworkResult.Success -> {
                        val newData = result.response.data.notices
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
                    is FetchNewsNetworkResult.Failure -> {
                        _errorMap[label] = "加载错误(${result.code}): ${result.msg}"
                    }
                    is FetchNewsNetworkResult.Error -> {
                        _errorMap[label] = result.msg
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "executeLoad Error", e)
                _errorMap[label] = e.localizedMessage ?: "未知错误"
            } finally {
                isLoading = false
                isRefreshing = false
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
            // 💡 传入两个 Repository
            return NewsViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}