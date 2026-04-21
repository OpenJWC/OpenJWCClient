package org.openjwc.client.ui.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.openjwc.client.R
import org.openjwc.client.net.models.FetchedNotice

@Preview
@Composable
fun TestNewsDetailScreen() {
    NewsDetailScreen(
        fetchedNotice = FetchedNotice(
            id = "123",
            label = "测试",
            title = "测试标题",
            date = "2023-03-18",
            detailUrl = "https://www.baidu.com",
            isPage = true,
            contentText = "测试文本",
            attachmentUrls = listOf(
                "https://www.bilibili.com", "https://www.baidu.com"
            ),
        ),
        onBack = {},
        onToBrowser = {},
        onAddToAttachments = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(
    fetchedNotice: FetchedNotice?,
    onBack: () -> Unit,
    onAddToAttachments: () -> Unit,
    onToBrowser: (String) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    val customImageLoader = remember {
        ImageLoader.Builder(context)
            .crossfade(true)
            // .placeholder(R.drawable.ic_image_placeholder)
            // .error(R.drawable.ic_image_error)
            .build()
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = fetchedNotice?.title ?: stringResource(R.string.news_not_found),
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (fetchedNotice != null) {
                        IconButton(onClick = onAddToAttachments) {
                            Icon(
                                Icons.AutoMirrored.Outlined.NoteAdd,
                                contentDescription = stringResource(R.string.add_to_attachments)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (fetchedNotice != null) {
                ExtendedFloatingActionButton(
                    onClick = { onToBrowser(fetchedNotice.detailUrl) },
                    icon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                    text = { Text(stringResource(R.string.view_in_browser)) },
                )
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (fetchedNotice == null) {
                Box {
                    Text(
                        text = stringResource(R.string.news_empty_egg),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.publish_date, fetchedNotice.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = stringResource(R.string.original_link, fetchedNotice.detailUrl),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onToBrowser(fetchedNotice.detailUrl) }
                    )
                }

                MarkdownText(
                    markdown = if (fetchedNotice.contentText.isNullOrBlank()) stringResource(R.string.no_detail_view_original) else fetchedNotice.contentText,
                    isTextSelectable = true,
                    linkColor = MaterialTheme.colorScheme.primary,
                    imageLoader = customImageLoader
                )
                Spacer(modifier = Modifier.padding(16.dp))

                AttachmentList(
                    fetchedNotice.attachmentUrls,
                    onAttachmentClick = onToBrowser,
                )
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun AttachmentList(
    urls: List<String>?,
    onAttachmentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (urls.isNullOrEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.attachment_list_count, urls.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column {
                urls.forEachIndexed { index, url ->
                    AttachmentItem(
                        url = url,
                        index = index,
                        onClick = { onAttachmentClick(url) }
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentItem(
    url: String,
    index: Int,
    onClick: () -> Unit
) {
    val fileName = url.substringAfterLast("/").ifBlank { stringResource(R.string.attachment_placeholder, index + 1) }

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent
    ) {
        ListItem(
            // 主内容：文件名
            headlineContent = {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {},
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Attachment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary // 强调色
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}