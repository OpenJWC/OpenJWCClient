package org.openjwc.client

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.navigation.NavGraph
import org.openjwc.client.ui.theme.OpenJWCClientTheme
import org.openjwc.client.ui.theme.ThemeConfig
import org.openjwc.client.work.NewsNotificationScheduler

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { ThemeConfig.isThemeChanging }

        enableEdgeToEdge()
        setContent {
            OpenJWCClientTheme {
                val context = LocalContext.current
                val settingsDataSource = remember { SettingsDataSource(context) }
                val settings by settingsDataSource.userSettings
                    .collectAsStateWithLifecycle(initialValue = null)

                // 设置变化时同步周期检查任务，并在开启时确保通知权限
                LaunchedEffect(settings?.newsNotificationEnabled, settings?.newsCheckIntervalMinutes) {
                    val s = settings ?: return@LaunchedEffect
                    NewsNotificationScheduler.sync(applicationContext)
                    if (s.newsNotificationEnabled && !hasNotificationPermission()) {
                        requestNotificationPermission()
                    }
                }

                NavGraph()
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
