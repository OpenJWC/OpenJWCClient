package org.openjwc.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.repository.ChatRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.navigation.NavGraph
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle
import org.openjwc.client.ui.theme.OpenJWCClientTheme
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.ChatViewModelFactory
import org.openjwc.client.viewmodels.MainViewModel
import org.openjwc.client.viewmodels.MainViewModelFactory
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.viewmodels.SettingsViewModelFactory
import kotlin.getValue

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(applicationContext)
        val settingsRepository = SettingsRepository(database.settingsDao())
        val chatRepository = ChatRepository(database.chatDao())
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val mainViewModel: MainViewModel by viewModels {
                MainViewModelFactory(settingsRepository)
            }
            val settingsViewModel: SettingsViewModel by viewModels {
                SettingsViewModelFactory(settingsRepository)
            }
            val chatViewModel: ChatViewModel by viewModels {
                ChatViewModelFactory(settingsRepository, chatRepository)
            }
            OpenJWCClientTheme(
                color = mainViewModel.themeColor.collectAsState(
                    initial = ColorType.Dynamic
                ).value,
                darkThemeStyle = mainViewModel.darkThemeStyle.collectAsState(
                    initial = DarkThemeStyle.Auto
                ).value
            ) {
                NavGraph(windowSizeClass, mainViewModel, settingsViewModel, chatViewModel)
            }
        }
    }
}
