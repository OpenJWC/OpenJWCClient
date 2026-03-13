package org.openjwc.client.navigation.me

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.openjwc.client.navigation.me.Routes.SETTINGS_PATTERN
import org.openjwc.client.ui.settings.ThemeScreen
import org.openjwc.client.ui.main.MainScreen
import org.openjwc.client.ui.settings.SettingsScreen
import org.openjwc.client.ui.theme.seedColors
import org.openjwc.client.viewmodels.MainViewModel
import androidx.compose.runtime.collectAsState
import org.openjwc.client.viewmodels.SettingsViewModel

private val LABEL = "MeNavGraph"

@Composable
fun NavGraph(
    windowSizeClass: WindowSizeClass
) {
    val mainViewModel: MainViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val navController = rememberNavController()

    val menus = settingsViewModel.menus.collectAsState().value

    Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN,
            // 进入新页面：从右往左轻微滑动 + 淡入
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 10 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) +
                        fadeIn(animationSpec = tween(300))
            },
            // 离开旧页面：向左轻微滑动 + 淡出
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 10 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) +
                        fadeOut(animationSpec = tween(300))
            },
            // 返回旧页面：从左往右轻微滑动 + 淡入
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 10 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) +
                        fadeIn(animationSpec = tween(300))
            },
            // 销毁当前页：向右轻微滑动 + 淡出
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
                    navController = navController
                )
            }

            composable(
                route = SETTINGS_PATTERN,
                arguments = listOf(navArgument("route") {
                    nullable = true
                    defaultValue = "default_menu"
                })
            ) { backStackEntry ->
                val route = backStackEntry.arguments?.getString("route") ?: return@composable // 找不到 route 就空白把
                Log.d(LABEL, "route: $route")
                when (route) {
                    "theme" ->
                        ThemeScreen(
                            onConfirm = { color, darkTheme ->
                                mainViewModel.updateThemeColor(color)
                                mainViewModel.updateDarkThemeStyle(darkTheme)
                                navController.popBackStack()
                            },
                            onBack = {
                                navController.popBackStack()
                                Log.d(LABEL, "onBack")
                            },
                            colorPresets = seedColors,
                            initialColorType = mainViewModel.themeColor.collectAsState().value,
                            initialThemeStyle = mainViewModel.darkThemeStyle.collectAsState().value
                        )

                    else -> {
                        /*val menu = menus.find { it.route == route }
                        if (menu != null) {
                            SettingsScreen(navController, menu)
                        } else {
                            Log.e(LABEL, "找不到对应的菜单项: $route")
                        }*/
                        SettingsScreen(navController, route, settingsViewModel)
                    }
                }
            }
        }
    }
}