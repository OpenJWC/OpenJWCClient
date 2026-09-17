package org.openjwc.client.ui.me

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Newspaper
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Upload
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openjwc.client.R
import org.openjwc.client.data.models.Motto
import org.openjwc.client.navigation.Screen
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget
import org.openjwc.client.viewmodels.MeViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MeScreenContent(
    navigator: Navigator,
    meViewModel: MeViewModel,
    motto: Motto,
    windowSizeClass: WindowSizeClass
) {
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val refreshing by meViewModel.refreshing.collectAsStateWithLifecycle()
    val onlineMode by meViewModel.onlineMode.collectAsStateWithLifecycle()
    val refreshAction = { meViewModel.refreshMotto() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    HitokotoView(
                        text = motto.text,
                        author = motto.author,
                        source = motto.source,
                        permalink = motto.permalink,
                        refreshing = refreshing,
                        onRefresh = if (onlineMode) refreshAction else null
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    menuSections(navigator)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "hitokoto_header") {
                    HitokotoView(
                        text = motto.text,
                        author = motto.author,
                        source = motto.source,
                        permalink = motto.permalink,
                        refreshing = refreshing,
                        onRefresh = if (onlineMode) refreshAction else null,
                        modifier = Modifier.padding(vertical = 64.dp, horizontal = 16.dp)
                    )
                }

                menuSections(navigator)

                item(key = "footer_spacer") {
                    Spacer(Modifier.height(88.dp))
                }
            }
        }
    }
}

private fun LazyListScope.menuSections(navigator: Navigator) {
    item(key = "menu_section_1") {
        SegmentedColumn {
            item {
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.Settings,
                    title = stringResource(R.string.settings),
                    onClick = { navigator.push(Screen.Settings) }
                )
            }
            item {
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.Newspaper,
                    title = stringResource(R.string.favorite_news),
                    onClick = { navigator.push(Screen.Favorite) }
                )
            }
        }
    }

    item(key = "menu_section_2") {
        SegmentedColumn {
            item {
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.Info,
                    title = stringResource(R.string.about),
                    onClick = { navigator.push(Screen.About) }
                )
            }
        }
    }
}

@Composable
fun HitokotoView(
    modifier: Modifier = Modifier,
    text: String,
    author: String? = null,
    source: String? = null,
    /** 在线一言的详情页；本地自定义文本为 null。 */
    permalink: String? = null,
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
) {
    var showActions by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val attribution = remember(author, source) {
        listOfNotNull(
            author?.takeIf { it.isNotBlank() },
            source?.takeIf { it.isNotBlank() }?.let { "《$it》" },
        ).joinToString(" ")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (onRefresh != null) showActions = !showActions
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.hitokoto_text_format, text),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.padding(16.dp))
        if (attribution.isNotEmpty()) {
            Text(
                text = stringResource(R.string.hitokoto_author_format, attribution),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
            )
        }
        // 在线一言按官方要求附上出处链接
        permalink?.let { link ->
            Text(
                text = stringResource(R.string.hitokoto_from_source),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable { uriHandler.openUri(link) }
            )
        }
        AnimatedVisibility(
            visible = showActions && onRefresh != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = {
                        onRefresh?.invoke()
                        showActions = false
                    },
                    enabled = !refreshing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.refresh))
                }
            }
        }
    }
}
