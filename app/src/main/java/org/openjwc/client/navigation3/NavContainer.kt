package org.openjwc.client.navigation3

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.launch
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
import org.openjwc.client.data.appPreferences
import org.openjwc.client.ui.animation.predictiveback.AOSPCrossActivityAnimation
import org.openjwc.client.ui.animation.predictiveback.KernelSUClassicPredictiveBackAnimation
import org.openjwc.client.ui.animation.predictiveback.MiuixPredictiveBackAnimation
import org.openjwc.client.ui.animation.predictiveback.NoPredictiveBackAnimation
import org.openjwc.client.ui.animation.predictiveback.PredictiveBackExitDirection
import org.openjwc.client.ui.animation.predictiveback.ScalePredictiveBackAnimation
import org.openjwc.client.ui.main.MainScreen
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
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
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
                                        .zIndex(-1f)
                                        .onGloballyPositioned { newCoordinates ->
                                            backgroundBlurAnchorCoordinates =
                                                newCoordinates.takeIf { coordinates ->
                                                    coordinates.isAttached
                                                }
                                        }
                                        .paint(
                                            painter = it,
                                            contentScale = ContentScale.Crop,
                                        )
                                        .drawWithContent {
                                            drawContent()
                                            drawRect(
                                                color = surfaceContainer.copy(
                                                    alpha = ThemeConfig.backgroundDim
                                                )
                                            )
                                        }
                                )
                            }

                            // 优化 2：预测性返回缩放动画只作用在 Content Wrapper 上
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .predictiveBackAnimationDecorator(
                                        gestureState?.transitionState,
                                        content.contentKey,
                                        navigator.current()
                                    )
                            ) {
                                content.Content()
                            }
                        }
                    }
                }
            }
        ),
        entryProvider = entryProvider {
            entry<Screen.Main> { MainScreen(navigator) }
            entry<Screen.Settings> { SettingsScreen(navigator) }
            entry<Screen.Theme> { ThemeScreen(navigator) }
            entry<Screen.ThemeSettings> { ThemeSettingsScreen(navigator) }
            entry<Screen.About> { AboutScreen(navigator) }
            entry<Screen.Host> { HostScreen(navigator) }
            entry<Screen.Login> { LoginScreen(navigator) }
            entry<Screen.Register> { RegisterScreen(navigator) }
            entry<Screen.Account> { AccountScreen(navigator) }
            entry<Screen.Language> { LanguageScreen(navigator) }
            entry<Screen.Review> { ReviewedNoticesScreen(navigator) }
            entry<Screen.NewsSettings> { NewsDisplaySettingsScreen(navigator) }
            entry<Screen.Policy> { PolicyScreen(navigator) }
            entry<Screen.License> { LicenseScreen(navigator) }
            entry<Screen.Log> { LogScreen(navigator) }
            entry<Screen.Favorite> { FavoriteScreen(navigator) }
            entry<Screen.NewsDetail> { NewsDetailScreen(navigator) }
            entry<Screen.UploadNews> { UploadNewsScreen(navigator) }
            entry<Screen.Load> { ImportWebViewScreen(navigator) }
            entry<Screen.TimetablePrefs> { TimetablePrefsScreen(navigator) }
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