package org.openjwc.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openjwc.client.navigation.me.NavGraph
import org.openjwc.client.ui.theme.OpenJWCClientTheme
import org.openjwc.client.viewmodels.MainViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val mainViewModel: MainViewModel = viewModel()
            OpenJWCClientTheme(
                color = mainViewModel.themeColor.collectAsState().value,
                darkThemeStyle = mainViewModel.darkThemeStyle.collectAsState().value
            ) {
                NavGraph(windowSizeClass)
            }
        }
    }
}
