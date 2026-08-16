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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.data.datastore.CachedHitokoto
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
    hitokoto: CachedHitokoto,
    windowSizeClass: WindowSizeClass
) {
    val successText = stringResource(R.string.refreshed_successfully)
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

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
                        text = hitokoto.text,
                        author = hitokoto.author,
                        onRefresh = { meViewModel.refreshHitokoto(successText) }
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
                        text = hitokoto.text,
                        author = hitokoto.author,
                        onRefresh = { meViewModel.refreshHitokoto(successText) },
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
            item {
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.History,
                    title = stringResource(R.string.reviewed_notices),
                    onClick = { navigator.push(Screen.Review) }
                )
            }
            item {
                SettingsJumpPageWidget(
                    icon = Icons.TwoTone.Upload,
                    title = stringResource(R.string.upload_news),
                    onClick = { navigator.push(Screen.UploadNews) }
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
    onRefresh: () -> Unit,
) {
    var showRefreshButton by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showRefreshButton = !showRefreshButton
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
        author?.let {
            Text(
                text = stringResource(R.string.hitokoto_author_format, it),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
            )
        }
        AnimatedVisibility(
            visible = showRefreshButton,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = {
                        onRefresh()
                        showRefreshButton = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.refresh))
                }
            }
        }
    }
}