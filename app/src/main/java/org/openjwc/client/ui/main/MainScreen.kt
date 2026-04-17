package org.openjwc.client.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import org.openjwc.client.navigation.Screen
import org.openjwc.client.ui.chat.ChatHistoryList
import org.openjwc.client.ui.chat.ChatScreen
import org.openjwc.client.ui.chat.EditMetadataDialog
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
    LocalUriHandler.current
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
    val historySessions by chatViewModel.allSessions.collectAsStateWithLifecycle(emptyList())
    val metadata by chatViewModel.currentSessionMetadata.collectAsState()
    val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showEditMetadataDialog by remember { mutableStateOf(false) }

    if (showEditMetadataDialog) {
        EditMetadataDialog(
            onDismiss = { showEditMetadataDialog = false },
            onConfirm = { newTitle ->
                showEditMetadataDialog = false
                chatViewModel.currentSessionMetadata.value?.let {
                    chatViewModel.updateMetadata(it.copy(title = newTitle))
                }
            },
            initialTitle = chatViewModel.currentSessionMetadata.value?.title ?: ""
        )
    }
    val drawerContent = @Composable {
        ModalDrawerSheet{
            ChatHistoryList(
                sessions = historySessions,
                currentSessionId = metadata?.sessionId,
                onSessionClick = { id ->
                    chatViewModel.loadSession(id)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    chatViewModel.toNewChat()
                    scope.launch { drawerState.close() }
                },
                onDeleteSession = { id -> chatViewModel.deleteSession(id) },
                onUpdateSessionMetadata = {
                    showEditMetadataDialog = true
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = currentTab == MainTab.Chat
    ) {
        Row(Modifier.fillMaxSize()) {
            if (useNavRail) {
                MainNavigationRail(currentTab) { mainViewModel.updateTab(it) }
            }

            Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier.weight(1f),
                topBar = {
                    TopAppBar(
                        title = {
                            when (currentTab) {
                                MainTab.Chat -> Text(text = chatTitle)
                                else -> Text(stringResource(currentTab.titleRes))
                            }
                        },
                        navigationIcon = {
                            if (currentTab == MainTab.Chat) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        },
                        actions = {
                            if (currentTab == MainTab.News) {
                                IconButton(onClick = { navController.navigate(Screen.Favorite) }) {
                                    Icon(Icons.Default.Star, contentDescription = "Favorite")
                                }
                                IconButton(onClick = { navController.navigate(Screen.UploadNews) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Upload News")
                                }
                            }
                        },
                        windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Top)
                    )
                },
                bottomBar = {
                    if (!useNavRail) MainNavigationBar(currentTab) { mainViewModel.updateTab(it) }
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { contentPadding ->
                MainTabContent(
                    mainViewModel = mainViewModel,
                    chatViewModel = chatViewModel,
                    newsViewModel = newsViewModel,
                    meViewModel = meViewModel,
                    currentTab = currentTab,
                    navController = navController,
                    contentPadding = contentPadding,
                    windowSizeClass = windowSizeClass
                )
            }
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
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
) {
    AnimatedContent(
        targetState = currentTab,
        label = "MainTabAnimation",
        transitionSpec = {
            (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                .togetherWith(fadeOut(animationSpec = tween(90)))
        },
        modifier = Modifier.fillMaxSize()
    ) { targetTab ->
        when (targetTab) {
            MainTab.Chat -> ChatScreen(
                windowSizeClass,
                chatViewModel,
                mainViewModel,
                contentPadding
            )

            MainTab.News -> NewsScreen(
                modifier = Modifier.padding(contentPadding),
                windowSizeClass,
                newsViewModel,
                mainViewModel,
                chatViewModel,
                navController
            )

            MainTab.Me -> MeScreen(
                modifier = Modifier.padding(contentPadding),
                windowSizeClass,
                meViewModel,
                navController
            )
        }
    }
}