package org.openjwc.client.ui.news

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.openjwc.client.net.models.FetchedNotice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    favorites: List<FetchedNotice>,
    windowSizeClass: WindowSizeClass,
    freshDays: Int,
    onBack: () -> Unit,
    onItemClick: (FetchedNotice) -> Unit,
    onAddToAttachments: (FetchedNotice) -> Unit,
    onDeleteFavorite: (FetchedNotice) -> Unit,
    onDeleteAllFavorites: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyGridState()

    var selectedNotice by remember { mutableStateOf<FetchedNotice?>(null) }
    val showMenu = selectedNotice != null
    val onMenuDismiss = { selectedNotice = null }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val columns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> GridCells.Fixed(3)
        WindowWidthSizeClass.Medium -> GridCells.Fixed(2)
        else -> GridCells.Fixed(1)
    }
    if (showDeleteAllDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要清空所有收藏的资讯吗？此操作不可撤销。") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onDeleteAllFavorites()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("全部删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("收藏的资讯") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 只有当列表不为空时才显示删除图标
                    if (favorites.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除所有收藏",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (favorites.isEmpty()) {
                EmptyFavoritesPlaceholder()
            } else {
                LazyVerticalGrid(
                    state = listState,
                    columns = columns,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(items = favorites, key = { index, item -> "${item.id} $index" }) { index, item ->
                        val isFresh = remember(item.id, freshDays) {
                            isDateFresh(item.date, freshDays)
                        }

                        Box {
                            InfoCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                fetchedNotice = item,
                                isFresh = isFresh,
                                isFavorited = true,
                                onClick = { onItemClick(item) },
                                onLongClick = { selectedNotice = item }, // 长按时记录当前选中的项
                                showLabel = true
                            )

                            DropdownMenu(
                                expanded = showMenu && selectedNotice === item,
                                onDismissRequest = onMenuDismiss
                            ) {
                                DropdownMenuItem(
                                    text = { Text("添加到附件") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Add, contentDescription = null)
                                    },
                                    onClick = {
                                        onAddToAttachments(item)
                                        onMenuDismiss()
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text("取消收藏", color = MaterialTheme.colorScheme.error)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        onDeleteFavorite(item)
                                        onMenuDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyFavoritesPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "暂无收藏内容",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "在资讯列表长按即可将感兴趣的内容收藏到这里",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}