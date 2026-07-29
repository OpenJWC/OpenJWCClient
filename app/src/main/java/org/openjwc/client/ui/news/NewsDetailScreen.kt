package org.openjwc.client.ui.news

import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget
import org.openjwc.client.viewmodels.NewsViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewsDetailScreen(navigator: Navigator, newsViewModel: NewsViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    val notice by newsViewModel.currentNewsToDisplay.collectAsState()

    fun openUrl(url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(notice?.title ?: stringResource(R.string.news_not_found), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (notice == null) {
            Column(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection).padding(innerPadding).padding(32.dp)) {
                Icon(Icons.TwoTone.Description, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.news_empty_egg), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            val n = notice!!
            Column(
                modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection).padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (n.date.isNotBlank()) Text(n.date, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                if (n.contentText != null && n.contentText.isNotBlank()) MarkdownText(markdown = n.contentText, isTextSelectable = true)
                else Text(stringResource(R.string.no_detail_view_original), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)

                val urls = n.attachmentUrls.orEmpty().filter { it.isNotBlank() }
                if (urls.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.attachment_list_count, urls.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    urls.forEach { url ->
                        SettingsBaseWidget(icon = Icons.TwoTone.Attachment, title = url.substringAfterLast("/").ifBlank { url }, modifier = Modifier.fillMaxWidth(), onClick = { openUrl(url) }) {}
                    }
                }
                if (n.detailUrl.isNotBlank()) {
                    SettingsJumpPageWidget(icon = Icons.TwoTone.OpenInNew, title = stringResource(R.string.view_in_browser), onClick = { openUrl(n.detailUrl) })
                }
            }
        }
    }
}
