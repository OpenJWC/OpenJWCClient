package org.openjwc.client.widget.ui

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders as material3ColorProviders
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import org.openjwc.client.data.appPreferences
import org.openjwc.client.ui.theme.ThemeSeedColors

@Composable
fun WidgetTheme(
    colors: ColorProviders,
    content: @Composable () -> Unit
) {
    GlanceTheme(
        colors = colors,
        content = content
    )
}

fun widgetColorProviders(context: Context, isDark: Boolean): ColorProviders {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return material3ColorProviders(
            light = dynamicLightColorScheme(context),
            dark = dynamicDarkColorScheme(context)
        )
    }

    val seedColor = context.appPreferences.getInt(
        "theme_seed_color",
        ThemeSeedColors.Default.toArgb()
    )
    val scheme = dynamicColorScheme(
        seedColor = Color(seedColor),
        isDark = isDark,
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2021
    )
    return material3ColorProviders(scheme)
}
