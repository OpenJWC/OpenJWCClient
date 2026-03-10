package org.openjwc.client.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openjwc.client.ui.chat.ChatScreen
import org.openjwc.client.ui.me.MeScreen
import org.openjwc.client.ui.news.NewsScreen
import org.openjwc.client.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview
@Composable
fun TestMainScreen() {
    MainScreen(
        windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(600.dp, 800.dp))
    )
}

@Composable
fun MainScreen(
    windowSizeClass: WindowSizeClass,
    mainViewModel: MainViewModel = viewModel()
) {
    MainScaffoldContent(
        windowSizeClass,
        mainViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffoldContent(
    windowSizeClass: WindowSizeClass,
    mainViewModel: MainViewModel
) {
    val currentTab by mainViewModel.currentTab.collectAsState()
    val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    Row(Modifier.fillMaxSize()) {
        if (useNavRail) {
            MainNavigationRail(currentTab) { mainViewModel.updateTab(it) }
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = {
                TopAppBar({ Text(stringResource(currentTab.titleRes)) })
            },
            bottomBar = {
                if (!useNavRail)
                    MainNavigationBar(currentTab) { mainViewModel.updateTab(it) }
            }
        ) { contentPadding ->
            MainTabContent(
                currentTab,
                contentPadding,
                windowSizeClass
            )
        }
    }
}

@Composable
private fun MainTabContent(
    currentTab: MainTab,
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
){
    Box(
        Modifier.fillMaxSize()
        .consumeWindowInsets(contentPadding)
    ) {
        when (currentTab) {
            MainTab.Chat -> ChatScreen(contentPadding, windowSizeClass)
            MainTab.News -> NewsScreen(contentPadding, windowSizeClass)
            MainTab.Me -> MeScreen(contentPadding, windowSizeClass)
        }
    }
}