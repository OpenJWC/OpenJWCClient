package org.openjwc.client.navigation3

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.CachedDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.data.repository.ChatRepository
import org.openjwc.client.data.repository.CourseRepository
import org.openjwc.client.data.repository.NewsRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.navigation.Screen
import org.openjwc.client.ui.me.settings.SettingsScreen
import org.openjwc.client.ui.me.settings.general.ThemeScreen
import org.openjwc.client.ui.me.settings.general.ThemeSettingsScreen
import org.openjwc.client.ui.me.AboutScreen
import org.openjwc.client.ui.me.settings.connection.HostScreen
import org.openjwc.client.ui.me.settings.auth.LoginScreen
import org.openjwc.client.ui.me.settings.auth.RegisterScreen
import org.openjwc.client.ui.me.settings.auth.AccountScreen
import org.openjwc.client.ui.me.settings.general.LanguageScreen
import org.openjwc.client.ui.me.ReviewedNoticesScreen
import org.openjwc.client.ui.me.settings.news.NewsDisplaySettingsScreen
import org.openjwc.client.ui.policy.PolicyScreen
import org.openjwc.client.ui.policy.LicenseScreen
import org.openjwc.client.ui.me.settings.log.LogScreen
import org.openjwc.client.ui.news.FavoriteScreen
import org.openjwc.client.ui.news.NewsDetailScreen
import org.openjwc.client.ui.news.upload.UploadNewsScreen
import org.openjwc.client.ui.timetable.load.ImportWebViewScreen
import org.openjwc.client.ui.me.settings.timetable.TimetablePrefsScreen
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.ChatViewModelFactory
import org.openjwc.client.viewmodels.MainViewModel
import org.openjwc.client.viewmodels.MainViewModelFactory
import org.openjwc.client.viewmodels.NewsViewModel
import org.openjwc.client.viewmodels.NewsViewModelFactory
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.viewmodels.SettingsViewModelFactory
import org.openjwc.client.viewmodels.TimetableViewModel
import org.openjwc.client.viewmodels.TimetableViewModelFactory
import org.openjwc.client.viewmodels.AuthViewModel
import org.openjwc.client.viewmodels.AuthViewModelFactory
import org.openjwc.client.viewmodels.MeViewModel
import org.openjwc.client.viewmodels.MeViewModelFactory
import org.openjwc.client.data.appPreferences
import org.openjwc.client.ui.animation.predictiveback.AOSPCrossActivityAnimation
import org.openjwc.client.ui.animation.predictiveback.KernelSUClassicPredictiveBackAnimation
import org.openjwc.client.ui.animation.predictiveback.MiuixPredictiveBackAnimation
import org.openjwc.client.ui.animation.predictiveback.NoPredictiveBackAnimation
import org.openjwc.client.ui.animation.predictiveback.PredictiveBackExitDirection
import org.openjwc.client.ui.animation.predictiveback.ScalePredictiveBackAnimation
import org.openjwc.client.ui.main.MainScreen
import org.openjwc.client.ui.main.UpdateDialog
import org.openjwc.client.ui.theme.ThemeConfig
import org.openjwc.client.ui.theme.backgroundImagePainter
import org.openjwc.client.ui.util.LocalBackgroundBlurAnchor
import org.openjwc.client.ui.util.LocalBlurState
import org.openjwc.client.ui.util.LocalSnackbarHost
import org.openjwc.client.navigation3.LocalNavigator
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported

@Composable
fun NavContainer() {
    val navigator = rememberNavigator(Screen.Main)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Shared ViewModels — created once at Activity scope
    val database = remember { AppDatabase.getDatabase(context) }
    val settingsDataSource = remember { SettingsDataSource(context) }
    val authDataSource = remember { AuthDataSource(context) }
    val cachedDataSource = remember { CachedDataSource(context) }
    val settingsRepository = remember { SettingsRepository(settingsDataSource, cachedDataSource, authDataSource, context) }
    val authRepository = remember { AuthRepository(authDataSource, settingsDataSource) }
    val chatRepository = remember { ChatRepository(database.chatDao(), settingsDataSource, authDataSource) }
    val newsRepository = remember { NewsRepository(database.newsDao(), settingsDataSource, authDataSource) }
    val courseRepository = remember { CourseRepository(database.courseDao(), database.tableDao()) }

    val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory(settingsRepository))
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(chatRepository))
    val newsViewModel: NewsViewModel = viewModel(factory = NewsViewModelFactory(settingsRepository, newsRepository, authRepository))
    val timetableViewModel: TimetableViewModel = viewModel(factory = TimetableViewModelFactory(courseRepository, settingsRepository))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsRepository, authRepository))
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
    val meViewModel: MeViewModel = viewModel(factory = MeViewModelFactory(settingsRepository))

    // 启动时检查更新
    LaunchedEffect(Unit) {
        mainViewModel.checkUpdate(showToast = false)
    }

    val showUpdate by mainViewModel.showUpdateDialog.collectAsState()
    val updateRelease = mainViewModel.updateRelease.collectAsState().value
    if (showUpdate && updateRelease != null) {
        Dialog(
            onDismissRequest = { mainViewModel.dismissUpdateDialog() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            UpdateDialog(
                gitHubRelease = updateRelease,
                onDismiss = { mainViewModel.dismissUpdateDialog() },
                onUpdate = {
                    mainViewModel.dismissUpdateDialog()
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateRelease.htmlUrl)))
                    } catch (_: Exception) {}
                }
            )
        }
    }

    val animType = ThemeConfig.predictiveBackAnimation
    val exitDir = ThemeConfig.predictiveBackExitDirection
    val predictiveBackAnimationHandler = remember(animType, exitDir) {
        val direction = try { PredictiveBackExitDirection.valueOf(exitDir) } catch (_: Exception) { PredictiveBackExitDirection.FOLLOW_GESTURE }
        when (animType) {
            "None" -> NoPredictiveBackAnimation()
            "Scale" -> ScalePredictiveBackAnimation(direction)
            "KernelSUClassic" -> KernelSUClassicPredictiveBackAnimation()
            "MIUIX" -> MiuixPredictiveBackAnimation()
            else -> AOSPCrossActivityAnimation(direction)
        }
    }

    var gestureState: NavigationEventState<SceneInfo<NavKey>>? = null
    val navigationScope = rememberCoroutineScope()

    val onBack: (() -> Unit) -> Unit = { callBack ->
        navigationScope.launch {
            predictiveBackAnimationHandler.onBackPressed(
                transitionState = gestureState?.transitionState,
                currentPageKey = navigator.current()
            )
            callBack()
            navigator.pop()
        }
    }

    // 优化 1：提出来复用 Blur Backdrop 状态，避免每次 Entry 切换重新创建组件
    val blurState = rememberMaterial3BlurBackdrop(ThemeConfig.isEnableBlur)

    val entries = rememberDecoratedNavEntries(
        backStack = navigator.backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            NavEntryDecorator(
                onPop = { key ->
                    predictiveBackAnimationHandler.onPagePop(
                        contentPageKey = key,
                        animationScope = navigationScope
                    )
                }
            ) { content ->
                val snackBarHostState = remember { SnackbarHostState() }
                var backgroundBlurAnchorCoordinates by remember {
                    mutableStateOf<LayoutCoordinates?>(null)
                }

                LaunchedEffect(backgroundImagePainter) {
                    if (backgroundImagePainter == null) {
                        backgroundBlurAnchorCoordinates = null
                    }
                }

                with(predictiveBackAnimationHandler) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .predictiveBackAnimationDecorator(
                                gestureState?.transitionState,
                                content.contentKey,
                                navigator.current()
                            )
                    ) {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer))
                        val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer

                        CompositionLocalProvider(
                            LocalBlurState provides blurState,
                            LocalSnackbarHost provides snackBarHostState,
                            LocalBackgroundBlurAnchor provides backgroundBlurAnchorCoordinates,
                        ) {
                            backgroundImagePainter?.let {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(0f)
                                        .onGloballyPositioned { newCoordinates ->
                                            backgroundBlurAnchorCoordinates =
                                                newCoordinates.takeIf { coordinates ->
                                                    coordinates.isAttached
                                                }
                                        }
                                        .paint(painter = it, contentScale = ContentScale.Crop)
                                        .drawWithContent {
                                            drawContent()
                                            drawRect(color = surfaceContainer.copy(alpha = ThemeConfig.backgroundDim))
                                        }
                                )
                            }

                            content.Content()
                        }
                    }
                }
            }
        ),
        entryProvider = entryProvider {
            entry<Screen.Main> { MainScreen(navigator, mainViewModel, chatViewModel, newsViewModel, timetableViewModel, settingsViewModel, meViewModel) }
            entry<Screen.Settings> { SettingsScreen(navigator, settingsViewModel, authViewModel, newsViewModel, timetableViewModel) }
            entry<Screen.Theme> { ThemeScreen(navigator) }
            entry<Screen.ThemeSettings> { ThemeSettingsScreen(navigator) }
            entry<Screen.About> { AboutScreen(navigator, mainViewModel) }
            entry<Screen.Host> { HostScreen(navigator, settingsViewModel) }
            entry<Screen.Login> { LoginScreen(navigator, authViewModel) }
            entry<Screen.Register> { RegisterScreen(navigator, authViewModel) }
            entry<Screen.Account> { AccountScreen(navigator, authViewModel, settingsViewModel) }
            entry<Screen.Language> { LanguageScreen(navigator, settingsViewModel) }
            entry<Screen.Review> { ReviewedNoticesScreen(navigator, newsViewModel) }
            entry<Screen.NewsSettings> { NewsDisplaySettingsScreen(navigator, settingsViewModel) }
            entry<Screen.Policy> { PolicyScreen(navigator) }
            entry<Screen.License> { LicenseScreen(navigator) }
            entry<Screen.Log> { LogScreen(navigator) }
            entry<Screen.Favorite> { FavoriteScreen(navigator, newsViewModel) }
            entry<Screen.NewsDetail> { NewsDetailScreen(navigator, newsViewModel) }
            entry<Screen.UploadNews> { UploadNewsScreen(navigator, newsViewModel) }
            entry<Screen.Load> { ImportWebViewScreen(navigator, timetableViewModel) }
            entry<Screen.TimetablePrefs> { TimetablePrefsScreen(navigator, settingsViewModel, timetableViewModel) }
        },
    )

    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = listOf(SinglePaneSceneStrategy()),
        sceneDecoratorStrategies = emptyList(),
        sharedTransitionScope = null,
        onBack = { onBack {} },
    )
    val scene = sceneState.currentScene

    val currentInfo = SceneInfo(scene)
    val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }
    gestureState = rememberNavigationEventState(
        currentInfo = currentInfo,
        backInfo = previousSceneInfos
    )

    NavigationBackHandler(
        state = gestureState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCompleted = { callBack -> onBack(callBack) },
        onBackCancelled = { callBack -> callBack() }
    )

    CompositionLocalProvider(
        LocalNavigator provides navigator
    ) {
    NavDisplay(
        sceneState = sceneState,
        navigationEventState = gestureState,
        contentAlignment = Alignment.TopStart,
        sizeTransform = null,
        predictivePopTransitionSpec = { swipeEdge ->
            with(predictiveBackAnimationHandler) {
                onPredictivePopTransitionSpec(swipeEdge = swipeEdge)
            }
        },
        popTransitionSpec = {
            with(predictiveBackAnimationHandler) { onPopTransitionSpec() }
        },
        transitionSpec = {
            with(predictiveBackAnimationHandler) { onTransitionSpec() }
        },
    )
    }
}

@Composable
fun rememberMaterial3BlurBackdrop(enableBlur: Boolean): LayerBackdrop? {
    if (!enableBlur || !isRenderEffectSupported()) return null
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer

    return rememberLayerBackdrop {
        if (ThemeConfig.isEnableBlurExp) {
            backgroundImagePainter?.let { painter ->
                with(painter) { draw(size = drawContext.size) }
            }
        } else {
            drawRect(backgroundColor)
        }
        drawRect(color = backgroundColor.copy(alpha = ThemeConfig.backgroundDim))
        drawContent()
    }
}