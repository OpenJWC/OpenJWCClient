package org.openjwc.client.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import kotlinx.coroutines.launch
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
    val chatTitle = chatViewModel.currentSessionMetadata.collectAsState().value?.title ?: "无标题"
    val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    Row(Modifier.fillMaxSize()) {
        if (useNavRail) {
            MainNavigationRail(currentTab) { mainViewModel.updateTab(it) }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.weight(1f),
            topBar = {
                TopAppBar({
                    when(currentTab) {
                        MainTab.Chat -> Text (text = chatTitle)
                        else -> Text(stringResource(currentTab.titleRes))
                    }
                },
                    navigationIcon = {
                        if(currentTab == MainTab.Chat)IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
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
                drawerState,
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
    drawerState: DrawerState,
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
){
    val metadata by chatViewModel.currentSessionMetadata.collectAsState()
    val sessionId = metadata?.sessionId
    Box(
        Modifier.fillMaxSize()
//        .consumeWindowInsets(contentPadding)
    ) {
        when (currentTab) {
            MainTab.Chat -> ChatScreen(sessionId, contentPadding, windowSizeClass, drawerState, chatViewModel)
            MainTab.News -> NewsScreen(contentPadding, windowSizeClass)
            MainTab.Me -> MeScreen(contentPadding, windowSizeClass, navController)
        }
    }
}