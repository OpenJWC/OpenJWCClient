package org.openjwc.client.navigation

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.openjwc.client.navigation.Routes.SETTINGS_PATTERN
import org.openjwc.client.ui.main.MainScreen
import org.openjwc.client.ui.me.AboutScreen
import org.openjwc.client.ui.me.settings.SettingsScreen
import org.openjwc.client.ui.me.settings.connection.AuthScreen
import org.openjwc.client.ui.me.settings.connection.HostScreen
import org.openjwc.client.ui.me.settings.general.ThemeScreen
import org.openjwc.client.ui.theme.seedColors
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.MainViewModel
import org.openjwc.client.viewmodels.SettingsViewModel

private const val LABEL = "NavGraph"

@Composable
fun NavGraph(
    windowSizeClass: WindowSizeClass,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    chatViewModel: ChatViewModel
) {
    val navController = rememberNavController()
    Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN,
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
            composable(Routes.MAIN) {
                MainScreen(
                    windowSizeClass,
                    navController = navController,
                    mainViewModel,
                    chatViewModel
                )
            }

            composable(
                route = SETTINGS_PATTERN,
                arguments = listOf(navArgument("route") {
                    nullable = true
                    defaultValue = "default_menu"
                })
            ) { backStackEntry ->
                val route = backStackEntry.arguments?.getString("route")
                    ?: return@composable // 找不到 route 就空白吧
                Log.d(LABEL, "route: $route")
                when (route) {
                    /** 目前的打算是把所有非主屏幕的 Route 都归结为 settings/{} */
                    "about" -> {
                        AboutScreen(
                            onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() }
                        )
                    }
                    "host" -> {
                        val currentSettings by settingsViewModel.settings.collectAsState()
                        HostScreen(
                            onConfirm = { host, port ->
                                settingsViewModel.updateHost(host)
                                settingsViewModel.updatePort(port)
                                navController.popBackStack()
                            },
                            onBack = {if (navController.previousBackStackEntry != null)navController.popBackStack()},
                            initialHost = currentSettings.host,
                            initialPort = currentSettings.port
                        )
                    }
                    "auth" -> {
                        val currentSettings by settingsViewModel.settings.collectAsState()
                        val deviceResult by settingsViewModel.deviceResult.collectAsState()
                        AuthScreen(
                            initialAuthKey = currentSettings.authKey,
                            onConfirm = { key ->
                                settingsViewModel.updateAuthKey(key)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() },
                            onRefreshDevices = {
                                settingsViewModel.devicesQuery()
                            },
                            thisDeviceId = currentSettings.uuidString,
                            onUnbindDevice = { /*TODO()*/},
                            devicesResult = deviceResult

                        )
                    }
                    "theme" -> {
                        val currentColor by mainViewModel.themeColor.collectAsState()
                        val currentStyle by mainViewModel.darkThemeStyle.collectAsState()

                        ThemeScreen(
                            onBack = { if (navController.previousBackStackEntry != null)navController.popBackStack() },
                            onSelect = { color, darkTheme ->
                                mainViewModel.updateThemeColor(color)
                                mainViewModel.updateDarkThemeStyle(darkTheme)
                            },
                            colorPresets = seedColors,
                            selectedColorType = currentColor,
                            darkThemeStyle = currentStyle
                        )
                    }

                    else -> {
                        SettingsScreen(
                            onRoute = { navController.navigate(it) },
                            onBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() },
                            route = route,
                            viewModel = settingsViewModel
                        )
                    }
                }
            }
        }
    }
}