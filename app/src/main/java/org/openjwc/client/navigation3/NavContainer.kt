package org.openjwc.client.navigation3

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.openjwc.client.data.datastore.LlmKeyStore
import org.openjwc.client.data.datastore.MottoCacheDataSource
import org.openjwc.client.data.datastore.LlmSettingsDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.source.SourceRegistry
import org.openjwc.client.data.source.SourceRunner
import org.openjwc.client.work.SourceCrawlScheduler
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.agent.AgentLoopFactory
import org.openjwc.client.data.repository.ChatRepository
import org.openjwc.client.data.repository.CourseRepository
import org.openjwc.client.data.repository.DailyReportRepository
import org.openjwc.client.data.repository.NewsRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.navigation.MainTab
import org.openjwc.client.navigation.Screen
import org.openjwc.client.ui.me.settings.SettingsScreen
import org.openjwc.client.ui.me.settings.general.ThemeScreen
import org.openjwc.client.ui.me.settings.general.ThemeSettingsScreen
import org.openjwc.client.ui.me.AboutScreen
import org.openjwc.client.ui.me.settings.llm.LlmSettingsScreen
import org.openjwc.client.ui.me.settings.motto.MottoSettingsScreen
import org.openjwc.client.ui.me.settings.sources.SourceDetailScreen
import org.openjwc.client.ui.me.settings.sources.SourceScriptScreen
import org.openjwc.client.ui.me.settings.sources.SourcesScreen
import org.openjwc.client.ui.me.settings.general.LanguageScreen
import org.openjwc.client.ui.me.settings.news.NewsDisplaySettingsScreen
import org.openjwc.client.ui.me.settings.notification.NotificationSettingsScreen
import org.openjwc.client.ui.me.settings.widget.WidgetSettingsScreen
import org.openjwc.client.ui.policy.PolicyScreen
import org.openjwc.client.ui.policy.LicenseScreen
import org.openjwc.client.ui.me.settings.log.LogScreen
import org.openjwc.client.ui.news.FavoriteScreen
import org.openjwc.client.ui.news.ImageViewerScreen
import org.openjwc.client.ui.news.NewsDetailScreen
import org.openjwc.client.ui.timetable.load.ImportWebViewScreen
import org.openjwc.client.ui.me.settings.timetable.TimetablePrefsScreen
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.ChatViewModelFactory
import org.openjwc.client.viewmodels.DailyReportViewModel
import org.openjwc.client.viewmodels.DailyReportViewModelFactory
import org.openjwc.client.viewmodels.LlmSettingsViewModel
import org.openjwc.client.viewmodels.LlmSettingsViewModelFactory
import org.openjwc.client.viewmodels.SourcesViewModel
import org.openjwc.client.viewmodels.SourcesViewModelFactory
import org.openjwc.client.viewmodels.MainViewModel
import org.openjwc.client.viewmodels.MainViewModelFactory
import org.openjwc.client.viewmodels.NewsViewModel
import org.openjwc.client.viewmodels.NewsViewModelFactory
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.viewmodels.SettingsViewModelFactory
import org.openjwc.client.viewmodels.TimetableViewModel
import org.openjwc.client.viewmodels.TimetableViewModelFactory
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
import org.openjwc.client.ui.util.LocalSharedTransitionScope
import org.openjwc.client.ui.util.LocalSnackbarHost
import org.openjwc.client.navigation3.LocalNavigator
import org.openjwc.client.notification.NewsNotificationContract
import org.openjwc.client.notification.NotificationNavigation
import org.openjwc.client.notification.ReminderBootstrapper
import org.openjwc.client.widget.WidgetDataManager

@Composable
fun NavContainer(
    launchIntent: Intent? = null,
    onLaunchIntentConsumed: () -> Unit = {},
) {
    val navigator = rememberNavigator(Screen.Main)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Shared ViewModels — created once at Activity scope
    val database = remember { AppDatabase.getDatabase(context) }
    val settingsDataSource = remember { SettingsDataSource(context) }
    val newsRepository = remember { NewsRepository(database.noticeDao(), database.sourceDao()) }
    val settingsRepository = remember { SettingsRepository(settingsDataSource, context) }
    val mottoCache = remember { MottoCacheDataSource(context) }
    val llmSettingsDataSource = remember { LlmSettingsDataSource(context) }
    val llmKeyStore = remember { LlmKeyStore(context) }
    val courseRepository = remember { CourseRepository(database.courseDao(), database.tableDao()) }
    val agentLoopFactory = remember {
        AgentLoopFactory(llmSettingsDataSource, llmKeyStore, newsRepository, courseRepository)
    }
    val chatRepository = remember { ChatRepository(database.chatDao(), agentLoopFactory) }
    val dailyReportRepository = remember {
        DailyReportRepository(database.dailyReportDao(), database.noticeDao(), agentLoopFactory)
    }
    // 日报仓库依赖 AgentLoopFactory，这里反向注入只读日报能力（避免构造循环）
    LaunchedEffect(agentLoopFactory, dailyReportRepository) {
        agentLoopFactory.dailyReportSource = dailyReportRepository
    }

    val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory(settingsRepository))
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(chatRepository))
    val dailyReportViewModel: DailyReportViewModel =
        viewModel(factory = DailyReportViewModelFactory(dailyReportRepository))
    val sourceRegistry = remember { SourceRegistry(context, database.sourceDao(), settingsDataSource) }
    val sourceRunner = remember {
        SourceRunner(
            sourceRegistry,
            database.sourceDao(),
            database.noticeDao(),
            SourceCrawlScheduler.newScriptHost(),
        )
    }
    val newsViewModel: NewsViewModel = viewModel(factory = NewsViewModelFactory(settingsRepository, newsRepository, sourceRunner))
    val timetableViewModel: TimetableViewModel = viewModel(factory = TimetableViewModelFactory(courseRepository, settingsRepository))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsRepository))
    val meViewModel: MeViewModel = viewModel(factory = MeViewModelFactory(settingsRepository, mottoCache))
    val llmSettingsViewModel: LlmSettingsViewModel = viewModel(
        factory = LlmSettingsViewModelFactory(llmSettingsDataSource, llmKeyStore)
    )
    val sourcesViewModel: SourcesViewModel = viewModel(
        factory = SourcesViewModelFactory(
            sourceRegistry,
            sourceRunner,
            database.sourceDao(),
            database.noticeDao(),
            settingsDataSource,
        )
    )

    // 通知/深链跳转
    LaunchedEffect(launchIntent) {
        val intent = launchIntent ?: return@LaunchedEffect
        when (intent.getStringExtra(NotificationNavigation.EXTRA_DESTINATION)) {
            NotificationNavigation.DEST_NEWS_DETAIL -> {
                mainViewModel.updateTab(MainTab.News)
                val id = intent.getStringExtra(NewsNotificationContract.EXTRA_NEWS_ID)
                if (!id.isNullOrBlank()) {
                    newsViewModel.noticeById(id)?.let { notice ->
                        newsViewModel.setCurrentNewsToDisplay(notice)
                        navigator.push(Screen.NoticeDetail)
                    }
                }
            }

            NotificationNavigation.DEST_NEWS -> mainViewModel.updateTab(MainTab.News)

            NotificationNavigation.DEST_TIMETABLE -> mainViewModel.updateTab(MainTab.Timetable)
        }
        onLaunchIntentConsumed()
    }

    // 启动时检查更新
    LaunchedEffect(Unit) {
        mainViewModel.checkUpdate(showToast = false)
    }

    // 订阅集合变化时重排后台抓取任务
    LaunchedEffect(Unit) {
        sourcesViewModel.sources
            .map { list -> list.filter { it.subscribed }.map { it.id }.sorted() }
            .distinctUntilChanged()
            .collect { SourceCrawlScheduler.sync(context) }
    }

    // 当前课表或课程变化时重排课程提醒并刷新桌面小组件
    LaunchedEffect(timetableViewModel) {
        combine(
            timetableViewModel.currentTable,
            timetableViewModel.currentTableCourses
        ) { table, courses -> table?.id to courses.size }
            .distinctUntilChanged()
            .collect {
                ReminderBootstrapper.rescheduleCurrentTimetable(context)
                WidgetDataManager.refreshWidget(context)
            }
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
            if (ThemeConfig.animationsEnabled) {
                predictiveBackAnimationHandler.onBackPressed(
                    transitionState = gestureState?.transitionState,
                    currentPageKey = navigator.current()
                )
            }
            callBack()
            navigator.pop()
        }
    }

    // 官方共享元素：SharedTransitionLayout 提供 SharedTransitionScope，
    // 它必须包住 rememberDecoratedNavEntries（各 entry 的装饰器）与 NavDisplay
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
    val sharedTransitionScope = this
    val entries = rememberDecoratedNavEntries(
        backStack = navigator.backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            NavEntryDecorator(
                onPop = { key ->
                    if (ThemeConfig.animationsEnabled) {
                        predictiveBackAnimationHandler.onPagePop(
                            contentPageKey = key,
                            animationScope = navigationScope
                        )
                    }
                }
            ) { content ->
                val snackBarHostState = remember { SnackbarHostState() }

                with(predictiveBackAnimationHandler) {
                    // 资讯详情页不加全局手势的图形变换（由共享元素负责形变）；
                    // 关闭动画时也不做手势变换
                    val pageDecorator = if (content.contentKey.isSharedElementRoute() || !ThemeConfig.animationsEnabled) {
                        Modifier
                    } else {
                        Modifier.predictiveBackAnimationDecorator(
                            gestureState?.transitionState,
                            content.contentKey,
                            navigator.current()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .then(pageDecorator)
                    ) {
                        // 资讯详情页不使用 entry 背景层：否则那层不透明底色既不参与共享元素动画
                        // （退出时收不回卡片），又会立刻盖住列表。详情页自己带底色。
                        val pageBackgroundAlpha = if (content.contentKey.isSharedElementRoute()) 0f else 1f
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer
                                        .copy(alpha = pageBackgroundAlpha)
                                )
                        )
                        val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer

                        CompositionLocalProvider(
                            LocalSnackbarHost provides snackBarHostState,
                        ) {
                            backgroundImagePainter?.let {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(0f)
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
            entry<Screen.Main> { MainScreen(navigator, mainViewModel, chatViewModel, dailyReportViewModel, newsViewModel, timetableViewModel, settingsViewModel, meViewModel) }
            entry<Screen.Settings> { SettingsScreen(navigator, settingsViewModel, timetableViewModel, llmSettingsViewModel, sourcesViewModel) }
            entry<Screen.Theme> { ThemeScreen(navigator) }
            entry<Screen.ThemeSettings> { ThemeSettingsScreen(navigator) }
            entry<Screen.LlmSettings> { LlmSettingsScreen(navigator, llmSettingsViewModel, settingsViewModel) }
            entry<Screen.Sources> { SourcesScreen(navigator, sourcesViewModel) }
            entry<Screen.SourceDetail> { SourceDetailScreen(navigator, sourcesViewModel) }
            entry<Screen.SourceScript> { SourceScriptScreen(navigator, sourcesViewModel) }
            entry<Screen.ImageViewer> { ImageViewerScreen(navigator, newsViewModel) }
            entry<Screen.MottoSettings> { MottoSettingsScreen(navigator, settingsViewModel) }
            entry<Screen.About> { AboutScreen(navigator, mainViewModel) }
            entry<Screen.Language> { LanguageScreen(navigator, settingsViewModel) }
            entry<Screen.NewsSettings> { NewsDisplaySettingsScreen(navigator, settingsViewModel) }
            entry<Screen.NotificationSettings> { NotificationSettingsScreen(navigator, settingsViewModel) }
            entry<Screen.WidgetSettings> { WidgetSettingsScreen(navigator) }
            entry<Screen.Policy> { PolicyScreen(navigator) }
            entry<Screen.License> { LicenseScreen(navigator) }
            entry<Screen.Log> { LogScreen(navigator) }
            entry<Screen.Favorite> { FavoriteScreen(navigator, newsViewModel) }
            entry<Screen.NewsDetail> { NewsDetailScreen(navigator, newsViewModel) }
            entry<Screen.NoticeDetail> { NewsDetailScreen(navigator, newsViewModel, sharedElement = false) }
            entry<Screen.Load> { ImportWebViewScreen(navigator, timetableViewModel) }
            entry<Screen.TimetablePrefs> { TimetablePrefsScreen(navigator, settingsViewModel, timetableViewModel) }
        },
    )

    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = listOf(SinglePaneSceneStrategy()),
        sceneDecoratorStrategies = emptyList(),
        sharedTransitionScope = sharedTransitionScope,
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
        LocalNavigator provides navigator,
        LocalSharedTransitionScope provides sharedTransitionScope,
    ) {
    NavDisplay(
        sceneState = sceneState,
        navigationEventState = gestureState,
        contentAlignment = Alignment.TopStart,
        sizeTransform = null,
        // 资讯详情页由共享元素负责「卡片↔详情」的形变，这里只做中性交叉淡入，
        // 避免全局转场风格（缩放/裁剪/位移）叠加在共享元素上互相打架。
        predictivePopTransitionSpec = { swipeEdge ->
            if (initialState.isNewsDetailScene() || !ThemeConfig.animationsEnabled) neutralContentTransform()
            else with(predictiveBackAnimationHandler) {
                onPredictivePopTransitionSpec(swipeEdge = swipeEdge)
            }
        },
        popTransitionSpec = {
            if (initialState.isNewsDetailScene() || !ThemeConfig.animationsEnabled) neutralContentTransform()
            else with(predictiveBackAnimationHandler) { onPopTransitionSpec() }
        },
        transitionSpec = {
            if (targetState.isNewsDetailScene() || !ThemeConfig.animationsEnabled) neutralContentTransform()
            else with(predictiveBackAnimationHandler) { onTransitionSpec() }
        },
    )
    }
    }
}

/** 该场景是否是「资讯详情」页。 */
private fun Scene<NavKey>.isNewsDetailScene(): Boolean =
    entries.any { it.contentKey.isSharedElementRoute() }

/**
 * 判断导航 key 是否走「共享元素」转场（资讯详情 / 图片查看器）。
 *
 * 预测返回动画选 `None` 时退化为普通页面：使用与其他页面一致的转场风格，
 * 卡片不再做共享元素形变。只有显式启用了预测返回动画（或整体动画开启）时才走共享元素。
 *
 * `@Parcelize object` 的 equals/toString 都基于实例，导航框架里可能不是同一个实例，
 * 因此这里做多重兜底比较。
 */
private fun Any?.isSharedElementRoute(): Boolean {
    // 全关动画、或预测返回动画为 None：按普通页面处理
    if (!ThemeConfig.animationsEnabled || ThemeConfig.predictiveBackAnimation == "None") return false
    if (this === Screen.NewsDetail || this === Screen.ImageViewer) return true
    val text = this?.toString() ?: return false
    return text.contains("NewsDetail") || text.contains("ImageViewer")
}

/** 中性转场：只做交叉淡入（不缩放/不位移/不裁剪），给共享元素留出表演空间。 */
private fun neutralContentTransform(): ContentTransform {
    // 关闭动画时用 0 时长，避免整页交叉淡入的开销
    val duration = if (ThemeConfig.animationsEnabled) SHARED_ELEMENT_TRANSITION_MILLIS else 0
    return ContentTransform(
        targetContentEnter = fadeIn(tween(duration)),
        initialContentExit = fadeOut(tween(duration)),
    )
}

private const val SHARED_ELEMENT_TRANSITION_MILLIS = 220
