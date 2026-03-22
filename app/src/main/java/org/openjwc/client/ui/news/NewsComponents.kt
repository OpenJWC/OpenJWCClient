package org.openjwc.client.ui.news

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openjwc.client.net.models.FetchedNotice
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsList(
    label: String,
    windowSizeClass: WindowSizeClass,
    newsItems: List<FetchedNotice>,
    freshDays: Int?,
    isLoading: Boolean,
    isEnd: Boolean,
    error: String?,

    showMenu: Boolean,
    selectedNotice: FetchedNotice?,
    onMenuDismiss: () -> Unit,
    onAddToAttachment: (FetchedNotice) -> Unit,

    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (FetchedNotice) -> Unit,
    onItemLongClick: (FetchedNotice) -> Unit,
    onInitialLoad: () -> Unit = {}
) {
    val listState = rememberLazyGridState()
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
                    val isFresh = remember(item.id, freshDays) {
                        isDateFresh(item.date, freshDays)
                    }

                    Box {
                        InfoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            fetchedNotice = item,
                            onClick = onItemClick,
                            onLongClick = onItemLongClick,
                            isFresh = isFresh
                        )

                        DropdownMenu(
                            expanded = showMenu && selectedNotice?.id == item.id,
                            onDismissRequest = onMenuDismiss
                        ) {
                            DropdownMenuItem(
                                text = { Text("添加到附件") },
                                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                                onClick = {
                                    onAddToAttachment(item)
                                    onMenuDismiss()
                                }
                            )
                        }
                    }
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
}


@Composable
fun ListFooter(isLoading: Boolean, isEnd: Boolean, error: String?, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(16.dp), contentAlignment = Alignment.Center
    ) {
        when {
            error != null -> {
                TextButton(onClick = onRetry) { Text("加载失败，点击重试") }
            }

            isEnd -> {
                Text(
                    "— 已加载全部内容 —",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            isLoading -> {
                LoadingIndicator(
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}


@Composable
fun EmptyErrorState(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(text = errorMessage, modifier = Modifier.padding(top = 8.dp))
        FilledTonalButton(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("重试") }
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
    modifier: Modifier = Modifier,
    fetchedNotice: FetchedNotice,
    onClick: (FetchedNotice) -> Unit,
    onLongClick: (FetchedNotice) -> Unit = {},
    isFresh: Boolean
) {
    Card(
        modifier = modifier
            .combinedClickable(
                onClick = { onClick(fetchedNotice) },
                onLongClick = { onLongClick(fetchedNotice) }
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFresh) 4.dp else 2.dp,
        ),
        border = null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ){
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = fetchedNotice.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isFresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isFresh) FontWeight.Bold else FontWeight.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (fetchedNotice.date.isNotEmpty()) {
                Text(
                    text = fetchedNotice.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EmptyLabelsPlaceholder(
    onRefresh: () -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 视觉焦点：一个带柔和背景的图标区
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Label,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "未发现资讯分类",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "暂时无法获取到新闻标签，请检查网络连接或稍后重试",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        FilledTonalButton(
            onClick = onRefresh,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("重新获取分类")
        }
    }
}

fun isDateFresh(dateString: String, freshDays: Int?): Boolean {
    if (freshDays == null || freshDays <= 0 || dateString.isBlank()) return false
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val noticeDate = LocalDate.parse(dateString, formatter)
        val today = LocalDate.now()
        val daysBetween = ChronoUnit.DAYS.between(noticeDate, today)
        daysBetween in 0..freshDays.toLong()
    } catch (e: Exception) {
        false
    }
}