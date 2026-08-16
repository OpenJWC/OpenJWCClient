package org.openjwc.client.ui.news

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openjwc.client.navigation3.Navigator
import kotlinx.coroutines.launch
import org.openjwc.client.R
import org.openjwc.client.data.appPreferences
import org.openjwc.client.navigation.Screen
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.net.models.toNoticeEntity
import org.openjwc.client.navigation.MainTab
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
    navController: Navigator
) {
    val tabs = newsViewModel.labels.collectAsStateWithLifecycle().value
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val isLoading = newsViewModel.isLoading.collectAsStateWithLifecycle().value
    val isRefreshing = newsViewModel.isRefreshing.collectAsStateWithLifecycle().value
    val labelError = newsViewModel.labelError.collectAsStateWithLifecycle().value
    var selectedNoticeForMenu by remember { mutableStateOf<FetchedNotice?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { newsViewModel.loadLabels() }

    Box(modifier = modifier) {
        if (tabs.isEmpty()) {
            if (!(isLoading || isRefreshing)) {
                EmptyLabelsPlaceholder(
                    onRefresh = { newsViewModel.loadLabels() },
                    errorMessage = labelError,
                    isLoggedIn = newsViewModel.needsAuth.collectAsStateWithLifecycle().value,
                    onToLogin = { navController.push(Screen.Login) }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(modifier = Modifier.size(128.dp))
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                SecondaryScrollableTabRow(selectedTabIndex = pagerState.currentPage, containerColor = Color.Transparent, edgePadding = 16.dp, divider = {},
                    indicator = { TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pagerState.currentPage), color = MaterialTheme.colorScheme.primary) }) {
                    tabs.forEachIndexed { index, label ->
                        val selected = pagerState.currentPage == index
                        Tab(selected = selected, onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(label, maxLines = 1, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium) })
                    }
                }
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { pageIndex ->
                    val currentLabel = tabs[pageIndex]
                    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { newsViewModel.loadCategory(currentLabel, isRefresh = true) }, modifier = Modifier.fillMaxSize()) {
                        val listState = rememberLazyGridState()
                        @OptIn(ExperimentalFoundationApi::class)
                        NewsList(label = currentLabel, windowSizeClass = windowSizeClass, listState = listState,
                            newsItems = newsViewModel.getNewsState(currentLabel), isLoading = isLoading, isEnd = newsViewModel.isEnd(currentLabel),
                            error = newsViewModel.getError(currentLabel), onRefresh = { newsViewModel.loadCategory(currentLabel, isRefresh = true) },
                            onLoadMore = { newsViewModel.loadNextPage(currentLabel) },
                            onItemClick = { newsViewModel.setCurrentNewsToDisplay(it); navController.push(Screen.NewsDetail) },
                            onItemLongClick = { selectedNoticeForMenu = it; showMenu = true },
                            showMenu = showMenu, selectedNotice = selectedNoticeForMenu, onMenuDismiss = { showMenu = false },
                            onAddToAttachment = { chatViewModel.addAttachment(it); mainViewModel.updateTab(MainTab.Chat) },
                            freshDays = newsViewModel.freshDays.collectAsStateWithLifecycle().value,
                            onInitialLoad = { newsViewModel.loadCategory(currentLabel) },
                            favoriteItems = newsViewModel.favoriteNews.collectAsStateWithLifecycle().value,
                            onDeleteFavorite = { newsViewModel.deleteFavorite(it.id) },
                            onAddToFavorite = { newsViewModel.insertFavorite(it.toNoticeEntity()) })
                        val showBackToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 5 } }
                        BackToTopButton(visible = showBackToTop, onClick = { scope.launch { listState.animateScrollToItem(0) } }, modifier = Modifier.align(Alignment.BottomEnd))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLabelsPlaceholder(isLoggedIn: Boolean, onRefresh: () -> Unit, onToLogin: () -> Unit, errorMessage: String?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.no_news_categories), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(if (isLoggedIn) stringResource(R.string.get_labels_failed_hint) else stringResource(R.string.not_logged_in), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (errorMessage != null) { Spacer(Modifier.height(8.dp)); Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(16.dp))
                if (isLoggedIn) FilledTonalButton(onClick = onRefresh) { Text(stringResource(R.string.refetch_categories)) }
                else FilledTonalButton(onClick = onToLogin) { Text(stringResource(R.string.login)) }
            }
        }
    }
}
