package org.openjwc.client.ui.news

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.openjwc.client.viewmodels.NewsViewModel

@Composable
fun NewsScreen(
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
    viewModel: NewsViewModel
) {
    val uriHandler = LocalUriHandler.current
    val tabs = viewModel.labels.collectAsStateWithLifecycle().value
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyGridState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tabs) {
        viewModel.loadLabels()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.loadLabels() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (tabs.isEmpty() && !viewModel.isLoading) {
                EmptyLabelsPlaceholder(
                    onRefresh = { viewModel.loadLabels() },
                    errorMessage = viewModel.labelError.collectAsStateWithLifecycle().value
                )
            } else if (tabs.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    SecondaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surface,
                        edgePadding = 16.dp,
                        divider = {},
                        indicator = {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, label ->
                            val isSelected = pagerState.currentPage == index
                            Tab(
                                selected = isSelected,
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = {
                                    Text(
                                        text = label,
                                        maxLines = 1, // 强制单行，触发滚动
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = if (isSelected)
                                            MaterialTheme.typography.titleSmall
                                        else
                                            MaterialTheme.typography.bodyMedium
                                    )
                                }
                            )
                        }
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { pageIndex ->
                        val currentLabel = tabs[pageIndex]
                        NewsList(
                            label = currentLabel,
                            windowSizeClass = windowSizeClass,
                            newsItems = viewModel.getNewsState(currentLabel),
                            listState = listState,
                            isLoading = viewModel.isLoading,
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
            }

            if (viewModel.isLoading && tabs.isEmpty()) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        val showBackToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 5 } }
        BackToTopButton(
            visible = showBackToTop,
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}