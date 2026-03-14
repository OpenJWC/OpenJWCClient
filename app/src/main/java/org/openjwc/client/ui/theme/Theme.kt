package org.openjwc.client.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.serialization.Serializable

@Serializable
enum class DarkThemeStyle {
    Auto,
    Light,
    Dark
}

@Composable
fun OpenJWCClientTheme(
    // Dynamic color is available on Android 12+
    color: ColorType,
    darkThemeStyle: DarkThemeStyle = DarkThemeStyle.Auto,
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkThemeStyle) {
        DarkThemeStyle.Auto -> isSystemInDarkTheme()
        DarkThemeStyle.Light -> false
        DarkThemeStyle.Dark -> true
    }

    val colorScheme = when {
        color is ColorType.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> rememberDynamicColorScheme(
            seedColor = when (color) {
                is ColorType.Dynamic -> SeedDefault
                is ColorType.Custom -> color.color
            },
            isDark = darkTheme
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}