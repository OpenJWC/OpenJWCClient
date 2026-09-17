package org.openjwc.client

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.navigation.NavGraph
import org.openjwc.client.notification.ReminderBootstrapper
import org.openjwc.client.ui.theme.OpenJWCClientTheme
import org.openjwc.client.ui.theme.ThemeConfig
import org.openjwc.client.widget.WidgetUpdateScheduler
import org.openjwc.client.work.DailyReportScheduler
import org.openjwc.client.work.NewsNotificationScheduler

class MainActivity : AppCompatActivity() {

    /** 通知/深链带入的 Intent；由 NavContainer 消费后清空。 */
    private val pendingLaunchIntent = MutableStateFlow<Intent?>(null)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { ThemeConfig.isThemeChanging }

        pendingLaunchIntent.value = intent

        enableEdgeToEdge()
        setContent {
            OpenJWCClientTheme {
                val context = LocalContext.current
                val settingsDataSource = remember { SettingsDataSource(context) }
                val settings by settingsDataSource.userSettings
                    .collectAsStateWithLifecycle(initialValue = null)

                // 设置变化时同步周期检查任务与课程提醒排程
                LaunchedEffect(
                    settings?.newsNotificationEnabled,
                    settings?.newsCheckIntervalMinutes,
                    settings?.courseReminderEnabled,
                    settings?.dailyReportEnabled,
                    settings?.dailyReportTime
                ) {
                    if (settings == null) return@LaunchedEffect
                    NewsNotificationScheduler.sync(applicationContext)
                    ReminderBootstrapper.rescheduleCurrentTimetable(applicationContext)
                    DailyReportScheduler.sync(applicationContext)
                }

                // 安排小组件的午夜刷新
                LaunchedEffect(Unit) {
                    WidgetUpdateScheduler.scheduleMidnightRefresh(applicationContext)
                }

                val launchIntent by pendingLaunchIntent.collectAsStateWithLifecycle()
                NavGraph(
                    launchIntent = launchIntent,
                    onLaunchIntentConsumed = { pendingLaunchIntent.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingLaunchIntent.value = intent
    }
}
