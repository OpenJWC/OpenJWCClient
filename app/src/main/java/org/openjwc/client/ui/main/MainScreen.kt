package org.openjwc.client.ui.main

import Screen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import org.openjwc.client.ui.chat.ChatScreen
import org.openjwc.client.ui.me.MeScreen
import org.openjwc.client.ui.news.NewsScreen
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.MainViewModel
import org.openjwc.client.viewmodels.MeViewModel
import org.openjwc.client.viewmodels.NewsViewModel
import java.io.File

@Composable
fun MainScreen(
    windowSizeClass: WindowSizeClass,
    navController: NavController,
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel,
    newsViewModel: NewsViewModel,
    meViewModel: MeViewModel,
    backgroundPath: String? = null,
    backgroundAlpha: Float = 1f
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        backgroundPath?.let { path ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(path))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = backgroundAlpha
            )
        }
        MainScaffoldContent(
            windowSizeClass,
            navController,
            mainViewModel,
            chatViewModel,
            newsViewModel,
            meViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffoldContent(
    windowSizeClass: WindowSizeClass,
    navController: NavController,
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel,
    newsViewModel: NewsViewModel,
    meViewModel: MeViewModel,
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
            containerColor = Color.Transparent,
            modifier = Modifier.weight(1f),
            topBar = {
                TopAppBar(
                    {
                        when (currentTab) {
                            MainTab.Chat -> Text(text = chatTitle)
                            else -> Text(stringResource(currentTab.titleRes))
                        }
                    },
                    navigationIcon = {
                        if (currentTab == MainTab.Chat) IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        when(currentTab) {
                            MainTab.News -> IconButton(
                                onClick = {
                                    navController.navigate(Screen.UploadNews)
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Upload News")
                            }
                            else -> {}
                        }
                    },
                    windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Top)
                )
            },
            bottomBar = {
                if (!useNavRail)
                    MainNavigationBar(currentTab) { mainViewModel.updateTab(it) }
            },
            contentWindowInsets = WindowInsets(0,0,0,0)
        ) { contentPadding ->
            MainTabContent(
                mainViewModel,
                chatViewModel,
                newsViewModel,
                meViewModel,
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
    mainViewModel: MainViewModel,
    chatViewModel: ChatViewModel,
    newsViewModel: NewsViewModel,
    meViewModel: MeViewModel,
    currentTab: MainTab,
    navController: NavController,
    drawerState: DrawerState,
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
) {
    val metadata by chatViewModel.currentSessionMetadata.collectAsState()
    val sessionId = metadata?.sessionId
    Box(
        Modifier.fillMaxSize()
    ) {
        when (currentTab) {
            MainTab.Chat -> ChatScreen(
                modifier = Modifier,
                sessionId,
                windowSizeClass,
                drawerState,
                chatViewModel,
                mainViewModel,
                contentPadding
            )

            MainTab.News -> NewsScreen(modifier = Modifier.padding(contentPadding), windowSizeClass, newsViewModel, mainViewModel, chatViewModel, navController)
            MainTab.Me -> MeScreen(modifier = Modifier.padding(contentPadding),windowSizeClass, meViewModel, navController)
        }
    }
}