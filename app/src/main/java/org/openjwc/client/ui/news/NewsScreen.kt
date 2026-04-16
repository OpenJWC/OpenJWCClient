package org.openjwc.client.ui.news

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.openjwc.client.navigation.Screen
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.ui.main.MainTab
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.MainViewModel
import org.openjwc.client.viewmodels.NewsViewModel

@Composable
fun NewsScreen(
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass,
    newsViewModel: NewsViewModel,
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel,
    navController: NavController
) {
    val tabs = newsViewModel.labels.collectAsStateWithLifecycle().value
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val isLoading = newsViewModel.isLoading.collectAsStateWithLifecycle().value
    val isRefreshing = newsViewModel.isRefreshing.collectAsStateWithLifecycle().value
    val labelError = newsViewModel.labelError.collectAsStateWithLifecycle().value
    var selectedNoticeForMenu by remember { mutableStateOf<FetchedNotice?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(tabs) {
        newsViewModel.loadLabels()
    }
    Box(
        modifier = modifier
    ) {

        if (tabs.isEmpty()) {
            if (!(isLoading || isRefreshing)) {
                EmptyLabelsPlaceholder(
                    onRefresh = { newsViewModel.loadLabels() },
                    errorMessage = labelError,
                    isLoggedIn = newsViewModel.needsAuth.collectAsStateWithLifecycle().value,
                    onToLogin = { navController.navigate(Screen.Login) }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                )
                {
                    LoadingIndicator(
                        modifier = Modifier.size(128.dp)
                    )
                }
            }
        } else {
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
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { newsViewModel.loadCategory(currentLabel, isRefresh = true) },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val listState = rememberLazyGridState()
                        NewsList(
                            label = currentLabel,
                            windowSizeClass = windowSizeClass,
                            listState = listState,
                            newsItems = newsViewModel.getNewsState(currentLabel),
                            isLoading = isLoading,
                            isEnd = newsViewModel.isEnd(currentLabel),
                            error = newsViewModel.getError(currentLabel),
                            onRefresh = { newsViewModel.loadCategory(currentLabel, isRefresh = true) },
                            onLoadMore = { newsViewModel.loadNextPage(currentLabel) },
                            onItemClick = { notice ->
                                navController.navigate(Screen.NewsDetail(notice))
                            },
                            onItemLongClick = { notice ->
                                selectedNoticeForMenu = notice
                                showMenu = true
                            },
                            showMenu = showMenu,
                            selectedNotice = selectedNoticeForMenu,
                            onMenuDismiss = { showMenu = false },
                            onAddToAttachment = { notice ->
                                chatViewModel.addAttachment(notice)
                                mainViewModel.updateTab(MainTab.Chat)
                            },
                            freshDays = newsViewModel.freshDays.collectAsStateWithLifecycle().value,
                            onInitialLoad = { newsViewModel.loadCategory(currentLabel) }
                        )
                        val showBackToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 5 } }
                        BackToTopButton(
                            visible = showBackToTop,
                            onClick = { scope.launch { listState.animateScrollToItem(0) } },
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }
            }
        }
    }
}
