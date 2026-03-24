package org.openjwc.client

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import cn.jpush.android.api.JPushInterface
import cn.jpush.android.api.JPushInterface.requestRequiredPermission
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.repository.ChatRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.navigation.NavGraph
import org.openjwc.client.ui.theme.OpenJWCClientTheme
import org.openjwc.client.viewmodels.ChatViewModel
import org.openjwc.client.viewmodels.ChatViewModelFactory
import org.openjwc.client.viewmodels.MainViewModel
import org.openjwc.client.viewmodels.MainViewModelFactory
import org.openjwc.client.viewmodels.MeViewModel
import org.openjwc.client.viewmodels.MeViewModelFactory
import org.openjwc.client.viewmodels.NewsViewModel
import org.openjwc.client.viewmodels.NewsViewModelFactory
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.viewmodels.SettingsViewModelFactory


class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val settingsRepository by lazy { SettingsRepository(database.settingsDao()) }
    private val chatRepository by lazy { ChatRepository(database.chatDao()) }

    private val mainViewModel: MainViewModel by viewModels { MainViewModelFactory(settingsRepository) }
    private val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModelFactory(settingsRepository) }
    private val chatViewModel: ChatViewModel by viewModels { ChatViewModelFactory(settingsRepository, chatRepository) }
    private val newsViewModel: NewsViewModel by viewModels { NewsViewModelFactory(settingsRepository) }
    private val meViewModel: MeViewModel by viewModels { MeViewModelFactory(settingsRepository) }
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        var isReady = false

        lifecycleScope.launch {
            mainViewModel.themeColor.first()
            mainViewModel.darkThemeStyle.first()
            isReady = true
        }

        splashScreen.setKeepOnScreenCondition { !isReady }
        requestRequiredPermission(this)
        val registrationId = JPushInterface.getRegistrationID(applicationContext)
        Log.d("MainActivity", "registrationId: $registrationId")
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            OpenJWCClientTheme(
                color = mainViewModel.themeColor.collectAsState(
                    initial = UserSettings().themeColor
                ).value,
                darkThemeStyle = mainViewModel.darkThemeStyle.collectAsState(
                    initial = UserSettings().themeStyle
                ).value
            ) {
                NavGraph(windowSizeClass, mainViewModel, settingsViewModel, chatViewModel, newsViewModel, meViewModel)
            }
        }
    }
}
