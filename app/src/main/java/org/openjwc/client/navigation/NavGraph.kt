package org.openjwc.client.navigation

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.openjwc.client.R
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.net.models.GitHubRelease
import org.openjwc.client.ui.main.MainScreen
import org.openjwc.client.ui.main.MainTab
import org.openjwc.client.ui.me.AboutScreen
import org.openjwc.client.ui.me.ReviewedNoticesScreen
import org.openjwc.client.ui.me.settings.SettingsScreen
import org.openjwc.client.ui.me.settings.connection.AuthScreen
import org.openjwc.client.ui.me.settings.connection.HostScreen
import org.openjwc.client.ui.me.settings.general.ThemeScreen
import org.openjwc.client.ui.me.settings.news.NewsDisplaySettingsScreen
import org.openjwc.client.ui.news.NewsDetailScreen
import org.openjwc.client.ui.news.upload.UploadNewsScreen
import org.openjwc.client.ui.policy.LicenseScreen
import org.openjwc.client.ui.policy.PolicyScreen
import org.openjwc.client.ui.theme.seedColors
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.MainViewModel
import org.openjwc.client.viewmodels.MeViewModel
import org.openjwc.client.viewmodels.NewsViewModel
import org.openjwc.client.viewmodels.SettingsViewModel
import kotlin.reflect.typeOf

val NoticeNavType = object : NavType<FetchedNotice>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): FetchedNotice? {
        return bundle.getString(key)?.let { Json.decodeFromString(it) }
    }

    override fun parseValue(value: String): FetchedNotice {
        return Json.decodeFromString(Uri.decode(value))
    }

    override fun serializeAsValue(value: FetchedNotice): String {
        return Uri.encode(Json.encodeToString(value))
    }

    override fun put(bundle: Bundle, key: String, value: FetchedNotice) {
        bundle.putString(key, Json.encodeToString(value))
    }
}

@Composable
fun NavGraph(
    windowSizeClass: WindowSizeClass,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    chatViewModel: ChatViewModel,
    newsViewModel: NewsViewModel,
    meViewModel: MeViewModel,
    backgroundPath: String? = null,
    backgroundAlpha: Float = 1f
) {
    val navController = rememberNavController()
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
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
                    backgroundAlpha = backgroundAlpha
                )
            }

            composable<Screen.NewsDetail>(
                typeMap = mapOf(
                    typeOf<FetchedNotice>() to NoticeNavType
                )
            ) { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.NewsDetail>()
                NewsDetailScreen(
                    fetchedNotice = args.fetchedNotice,
                    onBack = { navController.popBackStack() },
                    onToBrowser = { uri -> uriHandler.openUri(uri) },
                    onAddToAttachments = {
                        mainViewModel.updateTab(MainTab.Chat)
                        navController.popBackStack()
                        chatViewModel.addAttachment(args.fetchedNotice)
                    }
                )
            }

            composable<Screen.UploadNews> {
                val errorMessage = newsViewModel.uploadError.collectAsState().value
                val context = LocalContext.current
                val lifecycleScope = rememberCoroutineScope()
                UploadNewsScreen(
                    errorMessage = errorMessage,
                    onBack = {
                        if (navController.previousBackStackEntry != null)
                            navController.popBackStack()
                        newsViewModel.clearUploadError()
                    },
                    onUpload = {
                        lifecycleScope.launch {
                            val succeeded = newsViewModel.uploadNews(it)
                            if (succeeded) {
                                Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT)
                                    .show()
                                if (navController.previousBackStackEntry != null)
                                    navController.popBackStack()
                            }
                        }
                    }
                )
            }
            composable<Screen.Settings> {
                SettingsScreen(
                    onRoute = {
                        navController.navigate(it)
                    },
                    onBack = {
                        if (navController.previousBackStackEntry != null)
                            navController.popBackStack()
                    },
                    route = Screen.Settings,
                    viewModel = settingsViewModel
                )
            }
            composable<Screen.About> {
                var updateRelease: GitHubRelease? by remember { mutableStateOf(null) }
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onToGitHub = { uriHandler.openUri("https://github.com/OpenJWC") },
                    onRoute = {
                        navController.navigate(it)
                    },
                    onCheckForUpdate = {
                        Toast.makeText(context, "检查更新中……", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            updateRelease = mainViewModel.checkUpdate()
                            if (updateRelease == null) {
                                Toast.makeText(context, "未找到更新", Toast.LENGTH_SHORT).show()
                            }
                        }
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

            composable<Screen.Auth> {
                val currentSettings by settingsViewModel.settings.collectAsState()
                val deviceResult by settingsViewModel.deviceResult.collectAsState()
                val isLoadingDeviceIds by settingsViewModel.isLoadingDeviceResult.collectAsState()
                AuthScreen(
                    initialAuthKey = currentSettings.authKey,
                    onConfirm = { key ->
                        settingsViewModel.updateAuthKey(key)
                        settingsViewModel.deviceRegister()
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                    onRefreshDevices = {
                        settingsViewModel.clearUnbindResult()
                        settingsViewModel.devicesQuery()
                    },
                    thisDeviceId = currentSettings.uuidString,
                    onUnbindDevice = {
                        settingsViewModel.unbindAndRefresh(it)
                    },
                    devicesResult = deviceResult,
                    isLoadingDeviceIds = isLoadingDeviceIds,
                    unbindResult = settingsViewModel.deviceUnbindNetworkResult.collectAsState().value
                )
            }

            composable<Screen.Theme> {
                val currentColor = mainViewModel.uiState.collectAsState().value.themeColor
                val currentStyle = mainViewModel.uiState.collectAsState().value.darkThemeStyle
                val backgroundPath = settingsViewModel.settings.collectAsState().value.backgroundPath
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
        }
    }
}