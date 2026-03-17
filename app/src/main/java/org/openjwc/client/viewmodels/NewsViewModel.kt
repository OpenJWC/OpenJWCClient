
package org.openjwc.client.viewmodels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.collections.plus
import kotlin.collections.set

class NewsViewModel : ViewModel() {
    /*
    private val _newsCache = mutableStateMapOf<String, List<NewsItem>>()
    private val _pageMap = mutableMapOf<String, Int>()
    private val _isEndMap = mutableStateMapOf<String, Boolean>()

    private val _errorMap = mutableStateMapOf<String, String?>()

    var isLoading by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    fun getNewsState(path: String): List<NewsItem> = _newsCache[path] ?: emptyList()

    fun getError(path: String): String? = _errorMap[path]

    fun isEnd(path: String): Boolean = _isEndMap[path] ?: false

    fun loadCategory(path: String, isRefresh: Boolean = false) {
        if (!isRefresh && _newsCache.containsKey(path)) return
        executeLoad(path, 1, isRefresh)
    }

    fun loadNextPage(path: String) {
        if (_errorMap[path] != null) {
            _errorMap[path] = null
        }

        if (isLoading || isRefreshing || isEnd(path)) return

        val nextPage = (_pageMap[path] ?: 1) + 1
        executeLoad(path, nextPage, isRefresh = false)
    }

    private fun executeLoad(path: String, page: Int, isRefresh: Boolean) {
        if (isRefresh) isRefreshing = true else isLoading = true
        _errorMap[path] = null

        viewModelScope.launch {
            try {
                // 1. 获取封装后的结果对象
                val result = JwcCrawler.fetchNews(path, page)
                val newData = result.newsItems

                // 2. 使用爬虫解析出的 hasNextPage 来更新结尾状态
                // 只有当 hasNextPage 为 false 时，才标记为 End
                _isEndMap[path] = !result.hasNextPage

                if (isRefresh || page == 1) {
                    _newsCache[path] = newData
                    _pageMap[path] = 1
                    // 刷新时，如果数据不为空且爬虫说还有下一页，确保重置 End 状态
                    if (newData.isNotEmpty() && result.hasNextPage) {
                        _isEndMap[path] = false
                    }
                } else {
                    if (newData.isNotEmpty()) {
                        val currentList = _newsCache[path] ?: emptyList()
                        // 关键点：将新老数据合并后，根据 detailUrl 进行去重
                        val combinedList = (currentList + newData).distinctBy { it.detailUrl }

                        _newsCache[path] = combinedList
                        _pageMap[path] = page
                    }
                }

                // 额外保险：如果数据确实为空，强制标记为结束
                if (newData.isEmpty()) _isEndMap[path] = true
                _errorMap[path] = null

            } catch (e: Exception) {
                e.printStackTrace()
                // 网络异常时记录异常信息
                _errorMap[path] = e.localizedMessage ?: "网络异常，请重试"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }*/
}
