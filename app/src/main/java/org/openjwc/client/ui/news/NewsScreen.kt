package org.openjwc.client.ui.news

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import org.openjwc.client.viewmodels.NewsViewModel

@Composable
fun NewsScreen(
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
    viewModel: NewsViewModel
) {
    val currentLabel = "最新动态"
    val uriHandler = LocalUriHandler.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        NewsList(
            label = currentLabel,
            windowSizeClass = windowSizeClass,
            newsItems = viewModel.getNewsState(currentLabel),
            isLoading = viewModel.isLoading,
            isRefreshing = viewModel.isRefreshing,
            isEnd = viewModel.isEnd(currentLabel),
            error = viewModel.getError(currentLabel),
            onRefresh = { viewModel.loadCategory(currentLabel, isRefresh = true) },
            onLoadMore = { viewModel.loadNextPage(currentLabel) },
            onItemClick = { notice ->
                uriHandler.openUri(notice.detailUrl)
            },
            onInitialLoad = { viewModel.loadCategory(currentLabel) }
        )
    }
}