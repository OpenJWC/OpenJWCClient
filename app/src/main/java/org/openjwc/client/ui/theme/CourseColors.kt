package org.openjwc.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.blend.Blend
import com.materialkolor.palettes.TonalPalette

/**
 * 课程颜色由课程自身的种子色生成，并针对当前日/夜间模式选取合适的色调（tone）。
 *
 * - 种子色会先用 [Blend.harmonize] 向当前主题主色靠拢，从而与莫奈取色 / 自定义主题保持协调。
 * - 遵循 MD3 颜色角色：置于 surface 之上的彩色块使用 container 角色
 *   （浅色 tone 90 / 深色 tone 30），其上的文字使用 on-container
 *   （浅色 tone 10 / 深色 tone 90）。
 * - [accent] 对应 MD3 的强调色角色（浅色 tone 40 / 深色 tone 80），用于图标等小面积点缀。
 */
data class CourseColorPair(
    val container: Color,
    val content: Color,
    val accent: Color
)

object CourseColorGenerator {
    fun pair(seedArgb: Int, themePrimaryArgb: Int, isDark: Boolean): CourseColorPair {
        val harmonized = Blend.harmonize(seedArgb, themePrimaryArgb)
        val palette = TonalPalette.fromInt(harmonized)
        return CourseColorPair(
            container = Color(palette.tone(if (isDark) DARK_CONTAINER_TONE else LIGHT_CONTAINER_TONE)),
            content = Color(palette.tone(if (isDark) DARK_CONTENT_TONE else LIGHT_CONTENT_TONE)),
            accent = Color(palette.tone(if (isDark) DARK_ACCENT_TONE else LIGHT_ACCENT_TONE))
        )
    }

    // MD3 container role on surface
    private const val LIGHT_CONTAINER_TONE = 90
    private const val LIGHT_CONTENT_TONE = 10
    private const val DARK_CONTAINER_TONE = 30
    private const val DARK_CONTENT_TONE = 90

    // MD3 accent role (icons / small highlights)
    private const val LIGHT_ACCENT_TONE = 40
    private const val DARK_ACCENT_TONE = 80
}

@Composable
fun rememberCourseColor(seed: Color): CourseColorPair {
    val isDark = isInDarkTheme(ThemeConfig.forceDarkMode)
    val themePrimary = MaterialTheme.colorScheme.primary
    return remember(seed, isDark, themePrimary) {
        CourseColorGenerator.pair(seed.toArgb(), themePrimary.toArgb(), isDark)
    }
}
