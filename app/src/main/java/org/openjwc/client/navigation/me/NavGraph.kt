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
import org.openjwc.client.ui.me.settings.ThemeScreen
import org.openjwc.client.ui.main.MainScreen
import org.openjwc.client.ui.me.settings.SettingsScreen
import org.openjwc.client.ui.theme.seedColors
import org.openjwc.client.viewmodels.MainViewModel
import androidx.compose.runtime.collectAsState
import org.openjwc.client.ui.me.AboutScreen
import org.openjwc.client.viewmodels.SettingsViewModel

private const val LABEL = "MeNavGraph"

@Composable
fun NavGraph(
    windowSizeClass: WindowSizeClass
) {
    val mainViewModel: MainViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
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
                val route = backStackEntry.arguments?.getString("route") ?: return@composable // 找不到 route 就空白吧
                Log.d(LABEL, "route: $route")
                when (route) { /** 目前的打算是把所有非主屏幕的 Route 都归结为 settings/{} */
                    // 这个地方我先让所有屏幕都拿一个 navController，但是可能在根部操作更规范？
                    "about" -> {
                        AboutScreen(navController)
                    }
                    "theme" ->
                        ThemeScreen(
                            navController = navController,
                            onConfirm = { color, darkTheme ->
                                mainViewModel.updateThemeColor(color)
                                mainViewModel.updateDarkThemeStyle(darkTheme)
                                navController.popBackStack()
                            },
                            colorPresets = seedColors,
                            initialColorType = mainViewModel.themeColor.collectAsState().value,
                            initialThemeStyle = mainViewModel.darkThemeStyle.collectAsState().value
                        )

                    else -> {
                        SettingsScreen(navController, route, settingsViewModel)
                    }
                }
            }
        }
    }
}