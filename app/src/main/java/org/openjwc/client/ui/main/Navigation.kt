package org.openjwc.client.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun TestMainNavigationRail() {
    MainNavigationRail(currentTab = MainTab.Chat, onTabSelected = {})
}

@Composable
fun MainNavigationRail(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationRail {
        Column(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            Spacer(Modifier.weight(1f))
            MainTab.items.forEach { tab ->
                NavigationRailItem(
                    selected = currentTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = { Icon(tab.icon, null) },
                    label = { Text(stringResource(tab.titleRes)) }
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
fun TestMainNavigationBar() {
    MainNavigationBar(currentTab = MainTab.Chat, onTabSelected = {})
}

@Composable
fun MainNavigationBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar {
        MainTab.items.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, null) },
                label = { Text(stringResource(tab.titleRes)) }
            )
        }
    }
}