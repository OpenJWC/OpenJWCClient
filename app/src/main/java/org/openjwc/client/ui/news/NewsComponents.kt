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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.FetchedNotice
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsList(
    label: String,
    windowSizeClass: WindowSizeClass,
    listState: LazyGridState,
    newsItems: List<FetchedNotice>,
    favoriteItems: List<FetchedNotice>,
    freshDays: Int?,
    isLoading: Boolean,
    isEnd: Boolean,
    error: String?,

    showMenu: Boolean,
    selectedNotice: FetchedNotice?,
    onMenuDismiss: () -> Unit,
    onAddToAttachment: (FetchedNotice) -> Unit,
    onDeleteFavorite: (FetchedNotice) -> Unit,
    onAddToFavorite: (FetchedNotice) -> Unit,

    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (FetchedNotice) -> Unit,
    onItemLongClick: (FetchedNotice) -> Unit,
    onInitialLoad: () -> Unit = {}
) {
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
                itemsIndexed(items = newsItems, key = { index: Int, item: FetchedNotice -> "${item.id}_$index" }) {index, item ->
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
                            isFresh = isFresh,
                            isFavorited = favoriteItems.any { it.id == item.id }
                        )

                        DropdownMenu(
                            expanded = showMenu && selectedNotice === item,
                            onDismissRequest = onMenuDismiss
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_to_attachments)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Add,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    onAddToAttachment(item)
                                    onMenuDismiss()
                                }
                            )
                            if (favoriteItems.any { it.id == item.id }) {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.delete_favorite), color = MaterialTheme.colorScheme.error)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        onDeleteFavorite(item)
                                        onMenuDismiss()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.favorite)) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        onAddToFavorite(item)
                                        onMenuDismiss()
                                    }
                                )
                            }
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
                TextButton(onClick = onRetry) { Text(stringResource(R.string.load_failed_retry)) }
            }

            isEnd -> {
                Text(
                    stringResource(R.string.all_content_loaded),
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
        FilledTonalButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(stringResource(R.string.retry)) }
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
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.back_to_top))
        }
    }
}

@Preview(widthDp = 320, heightDp = 100)
@Composable
fun TestInfoCard() {
    InfoCard(
        modifier = Modifier,
        fetchedNotice = FetchedNotice(
            id = "123",
            label = "测试",
            title = "测试标题",
            date = "2023-03-18",
            detailUrl = "https://www.baidu.com",
            isPage = true,
            contentText = "测试正文",
            attachmentUrls = listOf("https://www.bilibili.com")
        ),
        isFresh = true,
        isFavorited = true,
        onClick = {},
        onLongClick = {},
        showLabel = true,
    )
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    fetchedNotice: FetchedNotice,
    isFresh: Boolean,
    isFavorited: Boolean,
    onClick: (FetchedNotice) -> Unit,
    onLongClick: (FetchedNotice) -> Unit = {},
    showLabel: Boolean = false,
) {
    val containerColor = if (isFresh) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val contentColor = contentColorFor(containerColor)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = { onClick(fetchedNotice) },
                onLongClick = { onLongClick(fetchedNotice) }
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFresh) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            if (showLabel) {
                Surface(
                    color = MaterialTheme.colorScheme.onSecondary,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = fetchedNotice.label,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = fetchedNotice.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isFresh) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isFresh) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(R.string.new_tag),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.onSecondary,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (fetchedNotice.date.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isFavorited) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(R.string.favorited),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(18.dp))
                    }
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = fetchedNotice.date,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLabelsPlaceholder(
    modifier: Modifier = Modifier,
    isLoggedIn: Boolean = false,
    onRefresh: () -> Unit,
    onToLogin: () -> Unit,
    errorMessage: String?,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
            text = stringResource(R.string.no_news_categories),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoggedIn) {
            Text(
                text = stringResource(R.string.get_labels_failed_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            Text(
                text = stringResource(R.string.not_logged_in),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        if (isLoggedIn) {
            FilledTonalButton(
                onClick = onRefresh,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(stringResource(R.string.refetch_categories))
            }
        } else {
            FilledTonalButton(onClick = onToLogin, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                Text(stringResource(R.string.login))
            }
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
        Logger.e("isDateFresh", e.localizedMessage ?: "Unknown Error")
        false
    }
}