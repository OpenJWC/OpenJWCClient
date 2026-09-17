package org.openjwc.client.ui.news

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.twotone.Newspaper
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import org.openjwc.client.ui.util.sharedBoundsWithNav
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.ui.theme.CardConfig

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewsList(
    label: String, windowSizeClass: WindowSizeClass, listState: LazyGridState,
    newsItems: List<FetchedNotice>, isLoading: Boolean, isEnd: Boolean, error: String?,
    onRefresh: () -> Unit, onLoadMore: () -> Unit, onItemClick: (FetchedNotice) -> Unit,
    onItemLongClick: (FetchedNotice) -> Unit, showMenu: Boolean, selectedNotice: FetchedNotice?,
    onMenuDismiss: () -> Unit, onAddToAttachment: (FetchedNotice) -> Unit,
    freshDays: Int?, onInitialLoad: () -> Unit,
    favoriteItems: List<FetchedNotice>, onDeleteFavorite: (FetchedNotice) -> Unit,
    onAddToFavorite: (FetchedNotice) -> Unit
) {
    LaunchedEffect(label) { onInitialLoad() }

    val shouldLoadMore by remember { derivedStateOf { val li = listState.layoutInfo; val last = li.visibleItemsInfo.lastOrNull()?.index ?: 0; !isLoading && !isEnd && li.totalItemsCount > 0 && last >= li.totalItemsCount - 2 } }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

    val columns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 1
        WindowWidthSizeClass.Medium -> 2
        WindowWidthSizeClass.Expanded -> 3
        else -> 1
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns), state = listState,
            modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(newsItems, key = { it.id }) { notice ->
                NewsCard(notice = notice, freshDays = freshDays,
                    isFavorited = favoriteItems.any { it.id == notice.id },
                    onClick = { onItemClick(notice) }, onLongClick = { onItemLongClick(notice) },
                    showMenu = showMenu && selectedNotice?.id == notice.id, onMenuDismiss = onMenuDismiss,
                    onAddToAttachment = { onAddToAttachment(notice) },
                    onFavoriteClick = { if (favoriteItems.any { it.id == notice.id }) onDeleteFavorite(notice) else onAddToFavorite(notice) })
            }

            if (newsItems.isEmpty() && !isLoading && error == null) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyCategoryPlaceholder() }
            }

            if (isLoading) item { Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp)) } }

            if (error != null) {
                item {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.load_failed_retry), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
            }

            if (isEnd && newsItems.isNotEmpty()) {
                item { Text(stringResource(R.string.all_content_loaded), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.fillMaxWidth().padding(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewsCard(notice: FetchedNotice, freshDays: Int?, isFavorited: Boolean,
             onClick: () -> Unit, onLongClick: () -> Unit,
             showMenu: Boolean, onMenuDismiss: () -> Unit,
             onAddToAttachment: () -> Unit, onFavoriteClick: () -> Unit) {
    val isFresh = isDateFresh(notice.date, freshDays)
    var showContextMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // 官方共享元素：与详情页同 key，卡片↔详情自动插值位置/尺寸并交叉淡入淡出
            .sharedBoundsWithNav("news-card-${notice.id}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true; onLongClick() },
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isFresh) MaterialTheme.colorScheme.primaryContainer.copy(alpha = CardConfig.cardAlpha) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = CardConfig.cardAlpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(notice.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = onFavoriteClick, modifier = Modifier.size(24.dp)) {
                        Icon(if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            if (isFavorited) stringResource(R.string.remove_from_favorites) else stringResource(R.string.favorite),
                            tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.add_to_attachments)) }, leadingIcon = { Icon(Icons.Default.Add, null) }, onClick = { showContextMenu = false; onAddToAttachment() })
                        DropdownMenuItem(text = { Text(if (isFavorited) stringResource(R.string.remove_from_favorites) else stringResource(R.string.favorite)) }, leadingIcon = { Icon(if (isFavorited) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null) }, onClick = { showContextMenu = false; onFavoriteClick() })
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(notice.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            if (!notice.contentText.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(notice.contentText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 某个栏目没有任何资讯时的空状态（本地语料可能还没抓到该栏目）。 */
@Composable
private fun EmptyCategoryPlaceholder() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.TwoTone.Newspaper,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.no_news_in_category),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.no_news_in_category_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BackToTopButton(visible: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(visible = visible, enter = fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec()), exit = scaleOut(motion.fastSpatialSpec()), modifier = modifier.padding(12.dp)) {
        FloatingActionButton(onClick = onClick) {
            Icon(Icons.Default.ArrowUpward, stringResource(R.string.back_to_top), Modifier.size(24.dp))
        }
    }
}

fun isDateFresh(dateString: String, freshDays: Int?): Boolean {
    if (freshDays == null || freshDays <= 0 || dateString.isBlank()) return false
    return try {
        val date = java.time.LocalDate.parse(dateString)
        java.time.temporal.ChronoUnit.DAYS.between(date, java.time.LocalDate.now()) <= freshDays
    } catch (e: Exception) { false }
}
