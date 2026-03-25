package org.openjwc.client

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.repository.ChatRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.SettingsDataSource
import org.openjwc.client.navigation.NavGraph
import org.openjwc.client.ui.policy.PolicyDialog
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
    private val settingsRepository by lazy { SettingsRepository(SettingsDataSource(applicationContext), applicationContext) }
    private val chatRepository by lazy { ChatRepository(database.chatDao()) }

    private val mainViewModel: MainViewModel by viewModels { MainViewModelFactory(settingsRepository) }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(
            settingsRepository
        )
    }
    private val chatViewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(
            settingsRepository,
            chatRepository
        )
    }
    private val newsViewModel: NewsViewModel by viewModels { NewsViewModelFactory(settingsRepository) }
    private val meViewModel: MeViewModel by viewModels { MeViewModelFactory(settingsRepository) }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition {
            !mainViewModel.uiState.value.isReady
        }
        enableEdgeToEdge()
        setContent {
            val state by mainViewModel.uiState.collectAsState()
            val windowSizeClass = calculateWindowSizeClass(this)
            if (state.isReady) {
                OpenJWCClientTheme(
                    color = state.themeColor,
                    darkThemeStyle = state.darkThemeStyle
                ) {
                    if (state.agreedPolicy == false) {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            PolicyDialog(
                                policyText = stringResource(R.string.policy_text),
                                onDismiss = { finish() },
                                onAgree = { mainViewModel.agreePolicy() }
                            )
                        }
                    } else {
                        NavGraph(
                            windowSizeClass,
                            mainViewModel,
                            settingsViewModel,
                            chatViewModel,
                            newsViewModel,
                            meViewModel,
                            state.backgroundPath,
                            state.backgroundAlpha
                        )
                    }
                }
            }
        }
    }
}
