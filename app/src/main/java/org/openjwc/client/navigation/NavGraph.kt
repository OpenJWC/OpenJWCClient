package org.openjwc.client.navigation

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.receiveAsFlow
import org.openjwc.client.R
import org.openjwc.client.data.settings.Menu
import org.openjwc.client.data.settings.MenuItem
import org.openjwc.client.data.settings.SettingSection
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.GitHubRelease
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.ui.main.MainScreen
import org.openjwc.client.ui.main.MainTab
import org.openjwc.client.ui.main.UpdateDialog
import org.openjwc.client.ui.me.AboutScreen
import org.openjwc.client.ui.me.ReviewedNoticesScreen
import org.openjwc.client.ui.me.settings.SettingsScreen
import org.openjwc.client.ui.me.settings.auth.AccountScreen
import org.openjwc.client.ui.me.settings.auth.LoginScreen
import org.openjwc.client.ui.me.settings.auth.RegisterScreen
import org.openjwc.client.ui.me.settings.connection.HostScreen
import org.openjwc.client.ui.me.settings.general.LanguageScreen
import org.openjwc.client.ui.me.settings.general.ThemeScreen
import org.openjwc.client.ui.me.settings.log.LogScreen
import org.openjwc.client.ui.me.settings.news.NewsDisplaySettingsScreen
import org.openjwc.client.ui.news.FavoriteScreen
import org.openjwc.client.ui.news.NewsDetailScreen
import org.openjwc.client.ui.news.upload.UploadNewsScreen
import org.openjwc.client.ui.policy.LicenseScreen
import org.openjwc.client.ui.policy.PolicyScreen
import org.openjwc.client.ui.theme.seedColors
import org.openjwc.client.viewmodels.AuthViewModel
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.MainViewModel
import org.openjwc.client.viewmodels.MeViewModel
import org.openjwc.client.viewmodels.NavEvent
import org.openjwc.client.viewmodels.NewsViewModel
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.viewmodels.UiEvent

@Composable
fun NavGraph(
    windowSizeClass: WindowSizeClass,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    chatViewModel: ChatViewModel,
    newsViewModel: NewsViewModel,
    meViewModel: MeViewModel,
    authViewModel: AuthViewModel,
    backgroundPath: String? = null,
    backgroundAlpha: Float = 1f
) {
    val navController = rememberNavController()
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val showUpdateDialog by mainViewModel.showUpdateDialog.collectAsState()
    val updateRelease by mainViewModel.updateRelease.collectAsState()

    LaunchedEffect(Unit) {
        mainViewModel.checkUpdate(false)
    }

    LaunchedEffect(Unit) {
        mainViewModel.updateEvent.collect {
            mainViewModel.showUpdateDialog.value = true
        }
    }

    LaunchedEffect(Unit) {
        chatViewModel.uiEvent.receiveAsFlow().collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.uiText.asString(context), Toast.LENGTH_SHORT).show()
                }

                is UiEvent.ShowSnackBar -> {
                    // TODO: 显示 SnackBar
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.uiEvent.receiveAsFlow().collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.uiText.asString(context), Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        meViewModel.uiEvent.receiveAsFlow().collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.uiText.asString(context), Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.uiEvent.receiveAsFlow().collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.uiText.asString(context), Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        chatViewModel.navEvent.receiveAsFlow().collect { event ->
            when (event) {
                is NavEvent.ToLogin -> {
                    navController.navigate(Screen.Login)
                }

                is NavEvent.ToRegister -> {
                    navController.navigate(Screen.Register)
                }

                is NavEvent.ToBack -> {
                    navController.popBackStack()
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        newsViewModel.navEvent.receiveAsFlow().collect { event ->
            when (event) {
                is NavEvent.ToLogin -> {
                    navController.navigate(Screen.Login)
                }

                is NavEvent.ToRegister -> {
                    navController.navigate(Screen.Register)
                }

                is NavEvent.ToBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.navEvent.receiveAsFlow().collect { event ->
            when (event) {
                is NavEvent.ToLogin -> {
                    navController.navigate(Screen.Login)
                }

                is NavEvent.ToRegister -> {
                    navController.navigate(Screen.Register)
                }

                is NavEvent.ToBack -> {
                    navController.popBackStack()
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        meViewModel.navEvent.receiveAsFlow().collect { event ->
            when (event) {
                is NavEvent.ToLogin -> {
                    navController.navigate(Screen.Login)
                }

                is NavEvent.ToRegister -> {
                    navController.navigate(Screen.Register)
                }

                is NavEvent.ToBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    if (showUpdateDialog && updateRelease != null) {
        UpdateDialog(
            gitHubRelease = updateRelease!!,
            onDismiss = mainViewModel::dismissUpdateDialog,
            onUpdate = {
                mainViewModel.dismissUpdateDialog()
                uriHandler.openUri(updateRelease!!.htmlUrl)
            }
        )
    }
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Main,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 10 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) +
                        fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 10 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) +
                        fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 10 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) +
                        fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 10 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) +
                        fadeOut(animationSpec = tween(300))
            }
        ) {
            composable<Screen.Policy> {
                PolicyScreen(
                    policyText = stringResource(id = R.string.policy_text),
                    onBack = { navController.popBackStack() },
                    onToBrowser = { uri -> uriHandler.openUri(uri) },
                )
            }

            composable<Screen.License> {
                LicenseScreen(
                    licenseText = stringResource(id = R.string.license),
                    onBack = { navController.popBackStack() },
                    onToBrowser = { uri -> uriHandler.openUri(uri) },
                )
            }
            composable<Screen.Main> {
                MainScreen(
                    windowSizeClass = windowSizeClass,
                    navController = navController,
                    mainViewModel = mainViewModel,
                    chatViewModel = chatViewModel,
                    newsViewModel = newsViewModel,
                    meViewModel = meViewModel,
                    backgroundPath = backgroundPath,
                    backgroundAlpha = backgroundAlpha,
                    settingsViewModel = settingsViewModel
                )
            }

            composable<Screen.NewsDetail> {
                val notice = newsViewModel.currentNewsToDisplay.collectAsState().value
                NewsDetailScreen(
                    fetchedNotice = notice,
                    onBack = {
                        navController.popBackStack()
                    },
                    onToBrowser = { uri -> uriHandler.openUri(uri) },
                    onAddToAttachments = {
                        val popped = navController.popBackStack<Screen.Main>(inclusive = false)
                        mainViewModel.updateTab(MainTab.Chat)
                        if (!popped) {
                            navController.navigate(Screen.Main) {
                                popUpTo<Screen.NewsDetail> { inclusive = true }
                            }
                        }
                        if (notice != null) {
                            chatViewModel.addAttachment(notice)
                        }
                    }
                )
            }
            composable<Screen.Favorite> {
                FavoriteScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = { notice ->
                        newsViewModel.setCurrentNewsToDisplay(notice)
                        navController.navigate(Screen.NewsDetail)
                    },
                    onAddToAttachments = {
                        val popped = navController.popBackStack<Screen.Main>(inclusive = false)
                        mainViewModel.updateTab(MainTab.Chat)
                        if (!popped) {
                            navController.navigate(Screen.Main) {
                                popUpTo<Screen.NewsDetail> { inclusive = true }
                            }
                        }
                        chatViewModel.addAttachment(it)
                    },
                    onDeleteFavorite = { newsViewModel.deleteFavorite(it.id) },
                    favorites = newsViewModel.favoriteNews.collectAsState().value,
                    freshDays = settingsViewModel.settings.collectAsState().value.freshDays,
                    windowSizeClass = windowSizeClass,
                    onDeleteAllFavorites = { newsViewModel.deleteAllFavorites() }
                )
            }
            composable<Screen.UploadNews> {
                val errorMessage = newsViewModel.uploadError.collectAsState().value
                UploadNewsScreen(
                    errorMessage = errorMessage,
                    onBack = {
                        if (navController.previousBackStackEntry != null)
                            navController.popBackStack()
                        newsViewModel.clearUploadError()
                    },
                    onUpload = newsViewModel::uploadNews
                )
            }
            composable<Screen.Settings> {
                val settingsText = stringResource(R.string.settings)
                val generalText = stringResource(R.string.general)
                val connectionText = stringResource(R.string.connection)
                val themeText = stringResource(R.string.theme)
                val newsText = stringResource(R.string.news)
                val hostText = stringResource(R.string.network_config)
                val accountText = stringResource(R.string.account_management)
                val logText = stringResource(R.string.log)
                val displaySettingsText = stringResource(R.string.display_settings)
                val debugText = stringResource(R.string.debug)
                val languageText = stringResource(R.string.language)
                val menuTemplates = remember {
                    Menu(
                        route = Screen.Settings, title = settingsText, sections = listOf(
                            SettingSection(
                                title = generalText, items = listOf(
                                    MenuItem.Route(
                                        icon = Icons.Default.Palette,
                                        route = Screen.Theme,
                                        title = themeText,
                                    ),
                                    MenuItem.Route(
                                        icon = Icons.Default.Language,
                                        route = Screen.Language,
                                        title = languageText,
                                    )
                                )
                            ), SettingSection(
                                title = connectionText, items = listOf(
                                    MenuItem.Route(
                                        icon = Icons.Default.Dns,
                                        route = Screen.Host,
                                        title = hostText,
                                    ),
                                    MenuItem.Route(
                                        icon = Icons.Default.VpnKey,
                                        route = Screen.Account,
                                        title = accountText,
                                    )
                                )
                            ), SettingSection(
                                title = newsText, items = listOf(
                                    MenuItem.Route(
                                        icon = Icons.Default.CalendarMonth,
                                        route = Screen.NewsSettings,
                                        title = displaySettingsText,
                                    )
                                )
                            ), SettingSection(
                                title = debugText, items = listOf(
                                    MenuItem.Route(
                                        icon = Icons.Default.BugReport,
                                        route = Screen.Log,
                                        title = logText,
                                    )
                                )
                            )
                        )

                    )
                }
                SettingsScreen(
                    onRoute = {
                        navController.navigate(it)
                    },
                    onBack = {
                        if (navController.previousBackStackEntry != null)
                            navController.popBackStack()
                    },
                    menu = menuTemplates,
                    onToggle = {
                        settingsViewModel.toggle(it)
                    }
                )
            }
            composable<Screen.About> {
                var updateRelease: GitHubRelease? by remember { mutableStateOf(null) }
                val context = LocalContext.current
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onToGitHub = { uriHandler.openUri("https://github.com/OpenJWC") },
                    onRoute = {
                        navController.navigate(it)
                    },
                    onCheckForUpdate = {
                        Toast.makeText(context, "检查更新中……", Toast.LENGTH_SHORT).show()
                        mainViewModel.checkUpdate(true)

                    },
                    updateRelease = updateRelease,
                    onUpdate = {
                        updateRelease?.let { uriHandler.openUri(it.htmlUrl) }
                    }
                )
            }

            composable<Screen.Host> {
                val currentSettings by settingsViewModel.settings.collectAsState()
                HostScreen(
                    onConfirm = { host, port, useHttp, proxy ->
                        settingsViewModel.updateHost(host)
                        settingsViewModel.updatePort(port)
                        settingsViewModel.updateUseHttp(useHttp)
                        settingsViewModel.updateProxy(proxy)
                        navController.popBackStack()
                    },
                    onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
                    initialHost = currentSettings.host,
                    initialPort = currentSettings.port,
                    initialUseHttp = currentSettings.useHttp,
                    initialProxy = currentSettings.proxy
                )
            }

            composable<Screen.Account> {
                val authSession by authViewModel.authSession.collectAsState()
                val deviceResult by settingsViewModel.deviceResult.collectAsState()
                val deviceUnbindResult by settingsViewModel.deviceUnbindNetworkResult.collectAsState()
                val isLoadingDeviceIds by settingsViewModel.isLoadingDeviceResult.collectAsState()
                AccountScreen(
                    onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
                    onRefreshDevices = {
                        settingsViewModel.clearUnbindResult()
                        settingsViewModel.devicesQuery()
                    },
                    isLoadingDeviceIds = isLoadingDeviceIds,
                    onUnbindDevice = { settingsViewModel.unbindAndRefresh(it) },
                    thisDeviceId = authSession.uuid,
                    devicesResult = deviceResult,
                    unbindResult = deviceUnbindResult,
                    onLogin = { navController.navigate(Screen.Login) },
                    onRegister = { navController.navigate(Screen.Register) },
                    onLogout = { authViewModel.logout() },
                    authSession = authSession
                )
            }
            composable<Screen.Language> {
                val settingsState by settingsViewModel.settings.collectAsState()
                LanguageScreen(
                    currentLanguageCode = settingsState.languageCode,
                    onBack = { navController.popBackStack() },
                    onLanguageSelect = { settingsViewModel.updateLanguage(it) }
                )
            }
            composable<Screen.Login> {
                val loginResult by authViewModel.loginResult.collectAsState()
                val isLoggingIn by authViewModel.isLoggingIn.collectAsState()
                LoginScreen(
                    isLoggingIn = isLoggingIn,
                    loginError = when (val result = loginResult) {
                        is NetworkResult.Failure -> "(${result.code}) ${result.msg}"
                        is NetworkResult.Error -> result.msg
                        else -> null
                    },
                    onLogin = { account, password ->
                        authViewModel.login(account, password)
                    },
                    onToRegisterScreen = {
                        navController.navigate(Screen.Register)
                    },
                    onBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable<Screen.Register> {
                val registerResult by authViewModel.registerResult.collectAsState()
                val isRegistering by authViewModel.isRegistering.collectAsState()
                RegisterScreen(
                    onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
                    onRegister = { username, password, email ->
                        authViewModel.register(username, password, email)
                    },
                    isRegistering = isRegistering,
                    registerError = when (val result = registerResult) {
                        is NetworkResult.Failure -> "(${result.code}) ${result.msg}"
                        is NetworkResult.Error -> result.msg
                        else -> null
                    }
                )
            }

            composable<Screen.Theme> {
                val currentColor = mainViewModel.uiState.collectAsState().value.themeColor
                val currentStyle = mainViewModel.uiState.collectAsState().value.darkThemeStyle
                val backgroundPath =
                    settingsViewModel.settings.collectAsState().value.backgroundPath
                val brightness = settingsViewModel.settings.collectAsState().value.backgroundAlpha

                ThemeScreen(
                    onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
                    onSelect = { color, darkTheme ->
                        mainViewModel.updateThemeColor(color)
                        mainViewModel.updateDarkThemeStyle(darkTheme)
                    },
                    colorPresets = seedColors,
                    selectedColorType = currentColor,
                    darkThemeStyle = currentStyle,
                    currentBackgroundPath = backgroundPath,
                    onSelectBackground = {
                        settingsViewModel.updateBackground(it)
                    },
                    onClearBackground = {
                        settingsViewModel.deleteBackground()
                    },
                    backgroundAlpha = brightness,
                    onAlphaChange = {
                        settingsViewModel.updateBackgroundAlpha(it)
                    }
                )
            }

            composable<Screen.Review> {
                val error by newsViewModel.reviewedNoticesError.collectAsState()
                val data by newsViewModel.reviewedNoticesData.collectAsState()
                ReviewedNoticesScreen(
                    error = error,
                    reviewedNoticesData = data,
                    onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
                    onRefresh = { newsViewModel.fetchReviewedNotices() }
                )
            }

            composable<Screen.NewsSettings> {
                NewsDisplaySettingsScreen(
                    initialFreshDays = settingsViewModel.settings.collectAsState().value.freshDays,
                    onSave = {
                        settingsViewModel.updateFreshDays(it)
                        navController.popBackStack()
                    },
                    onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() }
                )
            }

            composable<Screen.Log> {
                LogScreen(
                    logs = Logger.logHistory,
                    onBack = {
                        if (navController.previousBackStackEntry != null)
                            navController.popBackStack()
                    }
                )
            }
        }
    }
}