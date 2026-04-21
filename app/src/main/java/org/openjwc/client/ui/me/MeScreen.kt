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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.openjwc.client.R
import org.openjwc.client.data.settings.Event
import org.openjwc.client.data.settings.MenuItem
import org.openjwc.client.data.settings.SettingSection
import org.openjwc.client.navigation.Screen
import org.openjwc.client.ui.me.settings.MenuSectionCard
import org.openjwc.client.viewmodels.MeViewModel

@Composable
fun MeScreen(
    modifier: Modifier,
    windowSizeClass: WindowSizeClass,
    viewModel: MeViewModel,
    navController: NavController,
) {
    val sections =
        listOf(
            SettingSection(
                title = stringResource(R.string.contribution),
                items = listOf(
                    MenuItem.Route(
                        icon = Icons.Default.Add,
                        title = stringResource(R.string.upload_news),
                        route = Screen.UploadNews,
                    ),
                    MenuItem.Route(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.upload_results),
                        route = Screen.Review,
                    )
                )
            ),
            SettingSection(
                title = null,
                items = listOf(
                    MenuItem.Route(
                        icon = Icons.Default.Settings,
                        route = Screen.Settings,
                        title = stringResource(R.string.settings),
                    ),
                    MenuItem.Route(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.about),
                        route = Screen.About
                    ),
                )
            )
        )
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val hitokoto = viewModel.hitokoto.collectAsStateWithLifecycle().value
    val successText = stringResource(R.string.refreshed_successfully)
    LaunchedEffect(Unit) {
        viewModel.refreshHitokotoLazily()
    }
    Box(
        modifier = modifier,
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
                        onRefresh = {
                            viewModel.refreshHitokoto(successText)
                        }
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    menuSections(sections, navController)
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
                        onRefresh = {
                            viewModel.refreshHitokoto(successText)
                        },
                        modifier = Modifier.padding(vertical = 64.dp, horizontal = 16.dp)
                    )
                }
                menuSections(sections, navController)

                item(key = "footer_spacer") {
                    Spacer(Modifier.height(88.dp))
                }
            }
        }
    }
}

fun LazyListScope.menuSections(
    sections: List<SettingSection>,
    navController: NavController
) {
    items(
        items = sections,
        key = { section -> section.title ?: section.items.hashCode() }
    ) { section ->
        MenuSectionCard(
            section = section,
            onEvent = { event ->
                when (event) {
                    is Event.Route -> navController.navigate(event.route)
                    is Event.Action -> event.onAction()
                    else -> {}
                }
            }
        )
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
//            .animateContentSize()
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

@Preview
@Composable
fun TestHitokotoView() {
    HitokotoView(
        text = "逸一时，误一世！",
        author = "田所浩二",
        onRefresh = {}
    )
}