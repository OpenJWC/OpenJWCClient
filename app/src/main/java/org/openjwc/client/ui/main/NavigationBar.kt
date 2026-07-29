package org.openjwc.client.ui.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.openjwc.client.navigation.MainTab
import org.openjwc.client.ui.theme.CardConfig
import org.openjwc.client.ui.util.LocalHandlePageChange
import org.openjwc.client.ui.util.LocalSelectedPage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainNavigationBar(
    isBottomBar: Boolean
) {
    val tabs = MainTab.entries
    val page = LocalSelectedPage.current
    val handlePageChange = LocalHandlePageChange.current

    if (isBottomBar) {
        FlexibleBottomAppBar(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(CardConfig.cardAlpha),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = index == page,
                    onClick = { handlePageChange(index) },
                    icon = {
                        Icon(
                            if (index == page) tab.iconSelected else tab.iconNotSelected,
                            stringResource(tab.titleRes)
                        )
                    },
                    label = {
                        Text(
                            stringResource(tab.titleRes),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1, softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    },
                    alwaysShowLabel = false
                )
            }
        }
    } else {
        WideNavigationRail(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)),
            colors = WideNavigationRailColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(CardConfig.cardAlpha),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modalContainerColor = WideNavigationRailDefaults.colors().modalContainerColor,
                modalScrimColor = WideNavigationRailDefaults.colors().modalScrimColor,
                modalContentColor = WideNavigationRailDefaults.colors().modalContentColor,
            ),
        ) {
            tabs.forEachIndexed { index, tab ->
                WideNavigationRailItem(
                    railExpanded = false,
                    selected = index == page,
                    onClick = { handlePageChange(index) },
                    icon = {
                        Icon(
                            if (index == page) tab.iconSelected else tab.iconNotSelected,
                            stringResource(tab.titleRes)
                        )
                    },
                    label = {
                        Text(
                            stringResource(tab.titleRes),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1, softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private typealias WideNavigationRailColors = androidx.compose.material3.WideNavigationRailColors
