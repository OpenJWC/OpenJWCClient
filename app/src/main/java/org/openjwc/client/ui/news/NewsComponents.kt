package org.openjwc.client.ui.news

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.openjwc.client.net.models.Notice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsList(
    label: String,
    windowSizeClass: WindowSizeClass,
    newsItems: List<Notice>,

    isLoading: Boolean,
    isRefreshing: Boolean,
    isEnd: Boolean,
    error: String?,

    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (Notice) -> Unit,
    onInitialLoad: () -> Unit = {}
) {
    val listState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 进入页面或切换标签时触发初始加载
    LaunchedEffect(label) {
        onInitialLoad()
    }

    // 触底逻辑
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0)
            lastVisibleItemIndex > (totalItems - 3) && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoading && !isEnd) {
            onLoadMore()
        }
    }

    val columns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> GridCells.Fixed(3)
        WindowWidthSizeClass.Medium -> GridCells.Fixed(2)
        else -> GridCells.Fixed(1)
    }

    LaunchedEffect(error) {
        if (error != null && newsItems.isNotEmpty()) {
            snackbarHostState.showSnackbar(message = error, withDismissAction = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading && newsItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else if (error != null && newsItems.isEmpty()) {
                EmptyErrorState(errorMessage = error, onRetry = onRefresh)
            } else {
                LazyVerticalGrid(
                    state = listState,
                    columns = columns,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = newsItems, key = { it.id }) { item ->
                        InfoCard(
                            notice = item,
                            onClick = onItemClick,
                            isFresh = false
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ListFooter(
                            isLoading = isLoading,
                            isEnd = isEnd,
                            error = error,
                            onRetry = onLoadMore
                        )
                    }
                }
            }
        }

        val showBackToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 5 } }
        BackToTopButton(
            visible = showBackToTop,
            onClick = { scope.launch { listState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun ListFooter(isLoading: Boolean, isEnd: Boolean, error: String?, onRetry: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        when {
            error != null -> {
                TextButton(onClick = onRetry) { Text("加载失败，点击重试") }
            }
            isEnd -> {
                Text("— 已加载全部内容 —", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            isLoading -> {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

/**
 * 提取出来的全屏错误/空白状态
 */
@Composable
fun EmptyErrorState(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("😥", fontSize = 48.sp)
        Text(text = errorMessage, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("重试") }
    }
}

@Composable
fun BackToTopButton(visible: Boolean, onClick: () -> Unit, modifier: Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier.padding(24.dp)
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape
        ) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Top")
        }
    }
}
@Composable
fun InfoCard(
    notice: Notice,
    onClick: (Notice) -> Unit,
    isFresh: Boolean
) {
    Card(
        onClick = { onClick(notice) },
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFresh) 4.dp else 2.dp,
            pressedElevation = 6.dp
        ),
        border = null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isFresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if(isFresh) FontWeight.Bold else FontWeight.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (notice.date.isNotEmpty()) {
                Text(
                    text = notice.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End),
                    maxLines = 1
                )
            }
        }
    }
}