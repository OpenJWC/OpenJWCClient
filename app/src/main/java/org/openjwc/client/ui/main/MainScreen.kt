package org.openjwc.client.ui.main

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.openjwc.client.R
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.CachedDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.data.repository.ChatRepository
import org.openjwc.client.data.repository.CourseRepository
import org.openjwc.client.data.repository.NewsRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.navigation.MainTab
import org.openjwc.client.navigation.Screen
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.chat.ChatHistoryList
import org.openjwc.client.ui.chat.ChatMainContent
import org.openjwc.client.ui.chat.EditMetadataDialog
import org.openjwc.client.ui.me.MeScreenContent
import org.openjwc.client.ui.news.NewsScreen
import org.openjwc.client.ui.timetable.view.TimetableScreen
import org.openjwc.client.ui.theme.blurEffect
import org.openjwc.client.ui.theme.blurSource
import org.openjwc.client.ui.util.LocalHandlePageChange
import org.openjwc.client.ui.util.LocalSelectedPage
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.ChatViewModelFactory
import org.openjwc.client.viewmodels.MainViewModel
import org.openjwc.client.viewmodels.MainViewModelFactory
import org.openjwc.client.viewmodels.NewsViewModel
import org.openjwc.client.viewmodels.NewsViewModelFactory
import org.openjwc.client.viewmodels.TimetableViewModel
import org.openjwc.client.viewmodels.TimetableViewModelFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(navigator: Navigator) {
    val tabs = MainTab.entries
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as Activity)
    val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    val database = remember { AppDatabase.getDatabase(context) }
    val settingsDataSource = remember { SettingsDataSource(context) }
    val authDataSource = remember { AuthDataSource(context) }
    val cachedDataSource = remember { CachedDataSource(context) }

    val settingsRepository = remember { SettingsRepository(settingsDataSource, cachedDataSource, authDataSource, context) }
    val chatRepository = remember { ChatRepository(database.chatDao(), settingsDataSource, authDataSource) }
    val newsRepository = remember { NewsRepository(database.newsDao(), settingsDataSource, authDataSource) }
    val authRepository = remember { AuthRepository(authDataSource, settingsDataSource) }
    val courseRepository = remember { CourseRepository(database.courseDao(), database.tableDao()) }

    val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory(settingsRepository))
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(chatRepository))
    val newsViewModel: NewsViewModel = viewModel(factory = NewsViewModelFactory(settingsRepository, newsRepository, authRepository))
    val timetableViewModel: TimetableViewModel = viewModel(factory = TimetableViewModelFactory(courseRepository, settingsRepository))

    val currentTab by mainViewModel.currentTab.collectAsState()
    val selectedPage = tabs.indexOf(currentTab).coerceAtLeast(0)

    val handlePageChange: (Int) -> Unit = remember {
        { page -> mainViewModel.updateTab(tabs[page.coerceIn(tabs.indices)]) }
    }

    val chatTitle = chatViewModel.currentSessionMetadata.collectAsState().value?.title
        ?: stringResource(R.string.untitled)
    val historySessions by chatViewModel.allSessions.collectAsStateWithLifecycle(emptyList())
    val metadata by chatViewModel.currentSessionMetadata.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
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
        ModalDrawerSheet {
            Text(
                text = stringResource(R.string.chat_history),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(24.dp)
            )
            ChatHistoryList(
                sessions = historySessions,
                currentSessionId = metadata?.sessionId,
                onSessionClick = { id ->
                    chatViewModel.loadSession(id)
                    coroutineScope.launch { drawerState.close() }
                },
                onNewChat = {
                    chatViewModel.toNewChat()
                    coroutineScope.launch { drawerState.close() }
                },
                onDeleteSession = { id -> chatViewModel.deleteSession(id) },
                onUpdateSessionMetadata = { showEditMetadataDialog = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    val isChatTab = selectedPage == 0
    val isNewsTab = selectedPage == 1

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = isChatTab
    ) {
        Row(Modifier.fillMaxSize()) {
            if (useNavRail) {
                CompositionLocalProvider(
                    LocalHandlePageChange provides handlePageChange,
                    LocalSelectedPage provides selectedPage
                ) {
                    MainNavigationBar(isBottomBar = false)
                }
            }

            CompositionLocalProvider(
                LocalHandlePageChange provides handlePageChange,
                LocalSelectedPage provides selectedPage
            ) {
                Scaffold(
                    modifier = Modifier.weight(1f),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        TopAppBar(
                            modifier = Modifier,
                            title = {
                                Column {
                                    Text(
                                        when (isChatTab) {
                                            true -> chatTitle
                                            else -> stringResource(tabs[selectedPage].titleRes)
                                        }
                                    )
                                }
                            },
                            navigationIcon = {
                                if (isChatTab) {
                                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                }
                            },
                            actions = {
                                if (isNewsTab) {
                                    IconButton(onClick = { navigator.push(Screen.Favorite) }) {
                                        Icon(Icons.Default.Star, contentDescription = "Favorites")
                                    }
                                    IconButton(onClick = { navigator.push(Screen.UploadNews) }) {
                                        Icon(Icons.Default.Add, contentDescription = "Upload News")
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            ),
                            windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Top)
                        )
                    },
                    bottomBar = {
                        if (!useNavRail) {
                            MainNavigationBar(isBottomBar = true)
                        }
                    },
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    Box(Modifier.fillMaxSize().padding(innerPadding)) {
                        when (selectedPage) {
                            0 -> ChatMainContent(
                                chatViewModel = chatViewModel,
                                mainViewModel = mainViewModel,
                                windowSizeClass = windowSizeClass,
                                contentPadding = PaddingValues(top = 0.dp)
                            )
                            1 -> NewsScreen(
                                modifier = Modifier,
                                windowSizeClass = windowSizeClass,
                                newsViewModel = newsViewModel,
                                mainViewModel = mainViewModel,
                                chatViewModel = chatViewModel,
                                navController = navigator
                            )
                            2 -> TimetableScreen(
                                windowSizeClass = windowSizeClass,
                                viewModel = timetableViewModel,
                                onImportRequest = { navigator.push(Screen.Load) },
                                contentPadding = PaddingValues()
                            )
                            3 -> MeScreenContent(navigator)
                        }
                    }
                }
            }
        }
    }
}
