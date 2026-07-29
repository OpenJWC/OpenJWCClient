package org.openjwc.client

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.openjwc.client.navigation.NavGraph
import org.openjwc.client.ui.theme.OpenJWCClientTheme
import org.openjwc.client.ui.theme.ThemeConfig

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { ThemeConfig.isThemeChanging }

        enableEdgeToEdge()
        setContent {
            OpenJWCClientTheme {
                NavGraph()
            }
        }
    }
}
