package org.openjwc.client.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import org.openjwc.client.ui.chat.ChatScreen
import org.openjwc.client.ui.me.MeScreen
import org.openjwc.client.ui.news.NewsScreen
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.MainViewModel

@Composable
fun MainScreen(
    windowSizeClass: WindowSizeClass,
    navController: NavController,
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel
) {
    MainScaffoldContent(
        windowSizeClass,
        navController,
        mainViewModel,
        chatViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffoldContent(
    windowSizeClass: WindowSizeClass,
    navController: NavController,
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel
) {
    val currentTab by mainViewModel.currentTab.collectAsState()
    val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    Row(Modifier.fillMaxSize()) {
        if (useNavRail) {
            MainNavigationRail(currentTab) { mainViewModel.updateTab(it) }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
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
                chatViewModel,
                currentTab,
                navController,
                contentPadding,
                windowSizeClass,
            )
        }
    }
}

@Composable
private fun MainTabContent(
    chatViewModel: ChatViewModel,
    currentTab: MainTab,
    navController: NavController,
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
){
    val sessionId by chatViewModel.currentSessionId.collectAsState()
    Box(
        Modifier.fillMaxSize()
//        .consumeWindowInsets(contentPadding)
    ) {
        when (currentTab) {
            MainTab.Chat -> ChatScreen(sessionId, contentPadding, windowSizeClass, chatViewModel)
            MainTab.News -> NewsScreen(contentPadding, windowSizeClass)
            MainTab.Me -> MeScreen(contentPadding, windowSizeClass, navController)
        }
    }
}