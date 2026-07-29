package org.openjwc.client.ui.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.twotone.Attachment
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material.icons.twotone.Link
import androidx.compose.material.icons.twotone.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.openjwc.client.R
import org.openjwc.client.data.appPreferences
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget
import org.openjwc.client.ui.theme.blurEffect
import org.openjwc.client.ui.theme.blurSource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewsDetailScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val prefs = context.appPreferences

    val noticeId = prefs.getString("news_detail_id", null)
    val noticeTitle = prefs.getString("news_detail_title", stringResource(R.string.news_not_found))
        ?: stringResource(R.string.news_not_found)
    val noticeDate = prefs.getString("news_detail_date", "") ?: ""
    val noticeContent = prefs.getString("news_detail_content", "") ?: ""
    val noticeDetailUrl = prefs.getString("news_detail_url", "") ?: ""
    val attachmentCount = prefs.getInt("news_detail_attachment_count", 0)
    val attachments = remember {
        (0 until attachmentCount).map { i ->
            prefs.getString("news_detail_attachment_$i", "") ?: ""
        }.filter { it.isNotBlank() }
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.blurEffect(),
                title = { Text(noticeTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (noticeId == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(innerPadding)
                    .blurSource()
                    .padding(32.dp),
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Description,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.news_empty_egg),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(innerPadding)
                    .blurSource()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (noticeDate.isNotBlank()) {
                    Text(
                        text = noticeDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (noticeContent.isNotBlank()) {
                    MarkdownText(
                        markdown = noticeContent,
                        isTextSelectable = true
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_detail_view_original),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.attachment_list_count, attachments.size),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    attachments.forEach { url ->
                        SettingsBaseWidget(
                            icon = Icons.TwoTone.Attachment,
                            title = url.substringAfterLast("/").ifBlank { url },
                            modifier = Modifier.fillMaxWidth()
                        ) { openUrl(url) }
                    }
                }

                if (noticeDetailUrl.isNotBlank()) {
                    SettingsJumpPageWidget(
                        icon = Icons.TwoTone.OpenInNew,
                        title = stringResource(R.string.view_in_browser),
                        onClick = { _ -> openUrl(noticeDetailUrl) }
                    )
                }
            }
        }
    }
}
