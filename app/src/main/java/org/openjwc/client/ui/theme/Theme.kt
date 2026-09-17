package org.openjwc.client.ui.theme

import android.R
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.toBitmap
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.interfaces.MonetColorsChangedListener
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openjwc.client.data.appPreferences
import java.io.File
import java.io.FileOutputStream
import dev.kdrag0n.monet.theme.ColorScheme as MonetCompatColorScheme

@Stable
object ThemeConfig {
    // 主题状态
    var customBackgroundUri by mutableStateOf<Uri?>(null)
    var backgroundDim by mutableFloatStateOf(0f)
    var forceDarkMode by mutableStateOf<Boolean?>(null)
    var seedColor by mutableIntStateOf(ThemeSeedColors.Default.toArgb())
    var useDynamicColor by mutableStateOf(false)
    var monetCompatSeedColor by mutableIntStateOf(ThemeSeedColors.Default.toArgb())
    var dynamicColorSpec by mutableStateOf(ColorSpec.SpecVersion.SPEC_2021)
    var dynamicPaletteStyle by mutableStateOf(PaletteStyle.TonalSpot)

    // 背景状态
    var backgroundImageLoaded by mutableStateOf(false)
    var isThemeChanging by mutableStateOf(false)
    var preventBackgroundRefresh by mutableStateOf(false)
    var isHighContrastMode by mutableStateOf(false)
    var isUseBackgroundSeedColor by mutableStateOf(false)
    var predictiveBackAnimation by mutableStateOf("AOSP")
    var predictiveBackExitDirection by mutableStateOf("FOLLOW_GESTURE")

    /**
     * 界面动画强度：`full` 弹性动效（默认）、`standard` 标准、`off` 关闭。
     * 低配设备可降级，减少弹簧过冲/重测量带来的掉帧。
     */
    var animationMode by mutableStateOf(ANIMATION_FULL)

    /** 当前 motion scheme；`off` 时所有动画退化为瞬切（snap）。 */
    val motionScheme: MotionScheme
        get() = when (animationMode) {
            ANIMATION_STANDARD -> MotionScheme.standard()
            ANIMATION_OFF -> NoMotionScheme
            else -> MotionScheme.expressive()
        }

    /** 是否启用共享元素等较重的动画。 */
    val animationsEnabled: Boolean get() = animationMode != ANIMATION_OFF

    const val ANIMATION_FULL = "full"
    const val ANIMATION_STANDARD = "standard"
    const val ANIMATION_OFF = "off"

    // 主题变化检测
    private var lastDarkModeState: Boolean? = null

    fun detectThemeChange(currentDarkMode: Boolean): Boolean {
        val hasChanged = lastDarkModeState != null && lastDarkModeState != currentDarkMode
        lastDarkModeState = currentDarkMode
        return hasChanged
    }

    fun resetBackgroundState() {
        if (!preventBackgroundRefresh) {
            backgroundImageLoaded = false
        }
        isThemeChanging = true
    }

    fun updateTheme(
        seedColor: Int? = null,
        dynamicColor: Boolean? = null,
        darkMode: Boolean? = null
    ) {
        seedColor?.let { this.seedColor = it }
        dynamicColor?.let { useDynamicColor = it }
        darkMode?.let { forceDarkMode = it }
    }

    fun reset() {
        customBackgroundUri = null
        forceDarkMode = null
        seedColor = ThemeSeedColors.Default.toArgb()
        useDynamicColor = false
        monetCompatSeedColor = ThemeSeedColors.Default.toArgb()
        dynamicColorSpec = ColorSpec.SpecVersion.SPEC_2021
        dynamicPaletteStyle = PaletteStyle.TonalSpot
        backgroundImageLoaded = false
        isThemeChanging = false
        preventBackgroundRefresh = false
        lastDarkModeState = null
    }
}

/** 关闭动画：所有 motion spec 退化为瞬切，最省资源。 */
private object NoMotionScheme : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = snap()
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = snap()
}

object ThemeManager {
    fun saveThemeMode(context: Context, forceDark: Boolean?) {
        context.appPreferences.putString(
            "theme_mode", when (forceDark) {
                true -> "dark"
                false -> "light"
                null -> "system"
            }
        )
        ThemeConfig.forceDarkMode = forceDark
    }

    fun loadThemeMode(context: Context) {
        val mode = context.appPreferences.getString("theme_mode", "system")

        ThemeConfig.forceDarkMode = when (mode) {
            "dark" -> true
            "light" -> false
            else -> null
        }
    }

    /** 保存界面动画强度（full / standard / off）。 */
    fun saveAnimationMode(context: Context, mode: String) {
        context.appPreferences.putString("animation_mode", mode)
        ThemeConfig.animationMode = mode
    }

    fun saveSeedColor(context: Context, seedColor: Int) {
        context.appPreferences.putInt("theme_seed_color", seedColor)
        ThemeConfig.seedColor = seedColor
    }

    fun loadSeedColor(context: Context) {
        val prefs = context.appPreferences
        if (!prefs.contains("theme_seed_color")) {
            val legacyThemeName = prefs.getString("theme_colors", "default") ?: "default"
            val migratedSeedColor = ThemeSeedColors.fromLegacyNameArgb(legacyThemeName)
            prefs.putInt("theme_seed_color", migratedSeedColor)
            ThemeConfig.seedColor = migratedSeedColor
            return
        }

        ThemeConfig.seedColor = prefs.getInt(
            "theme_seed_color",
            ThemeSeedColors.Default.toArgb()
        )
    }

    fun saveDynamicColorState(context: Context, enabled: Boolean) {
        context.appPreferences.putBoolean("use_dynamic_color", enabled)
        ThemeConfig.useDynamicColor = enabled
    }


    fun loadDynamicColorState(context: Context) {
        val enabled = context.appPreferences.getBoolean(
            "use_dynamic_color",
            true
        )
        ThemeConfig.useDynamicColor = enabled
    }

    fun saveDynamicColorSpec(context: Context, spec: ColorSpec.SpecVersion) {
        context.appPreferences.putString("dynamic_color_spec", spec.name)
        ThemeConfig.dynamicColorSpec = spec
    }

    fun loadDynamicColorSpec(context: Context) {
        val specName = context.appPreferences.getString(
            "dynamic_color_spec",
            ColorSpec.SpecVersion.SPEC_2021.name
        )
        ThemeConfig.dynamicColorSpec = ColorSpec.SpecVersion.entries
            .find { it.name == specName }
            ?: ColorSpec.SpecVersion.SPEC_2021
    }

    fun saveDynamicPaletteStyle(context: Context, style: PaletteStyle) {
        context.appPreferences.putString("dynamic_palette_style", style.name)
        ThemeConfig.dynamicPaletteStyle = style
    }

    fun loadDynamicPaletteStyle(context: Context) {
        val styleName = context.appPreferences.getString(
            "dynamic_palette_style",
            PaletteStyle.TonalSpot.name
        )
        ThemeConfig.dynamicPaletteStyle = PaletteStyle.entries
            .find { it.name == styleName }
            ?: PaletteStyle.TonalSpot
    }
}

object BackgroundManager {
    private const val TAG = "BackgroundManager"

    fun saveBackgroundDim(context: Context, dim: Float) {
        ThemeConfig.backgroundDim = dim
        context.appPreferences.putFloat("background_dim", dim)
    }

    fun saveUseBackgroundSeedColor(context: Context, enable: Boolean) {
        ThemeConfig.isUseBackgroundSeedColor = enable
        context.appPreferences.putBoolean("use_background_seed_color", enable)
    }

    fun saveEnableHighContrastMode(context: Context, enable: Boolean) {
        ThemeConfig.isHighContrastMode = enable
        context.appPreferences.putBoolean("high_contrast_mode", enable)
    }

    fun saveAndApplyCustomBackground(
        context: Context,
        uri: Uri
    ) {
        try {
            val finalUri = copyImageToInternalStorage(context, uri)

            saveBackgroundUri(context, finalUri)
            ThemeConfig.customBackgroundUri = finalUri
            CardConfig.updateBackground(true)
            resetBackgroundState(context)

        } catch (e: Exception) {
            Log.e(TAG, "保存背景失败: ${e.message}", e)
        }
    }

    fun clearCustomBackground(context: Context) {
        saveBackgroundUri(context, null)
        ThemeConfig.customBackgroundUri = null
        CardConfig.updateBackground(false)
        resetBackgroundState(context)
    }

    fun loadCustomBackground(context: Context) {
        val prefs = context.appPreferences
        val uriString = prefs.getString("custom_background", null)

        val newUri = uriString?.toUri()
        val preventRefresh = prefs.getBoolean("prevent_background_refresh", false)

        ThemeConfig.preventBackgroundRefresh = preventRefresh

        if (!preventRefresh || ThemeConfig.customBackgroundUri?.toString() != newUri?.toString()) {
            Log.d(TAG, "加载自定义背景: $uriString")
            ThemeConfig.customBackgroundUri = newUri
            ThemeConfig.backgroundImageLoaded = false
            CardConfig.updateBackground(newUri != null)
        }

        ThemeConfig.backgroundDim = prefs.getFloat("background_dim", 0f).coerceIn(0f, 1f)
        ThemeConfig.isUseBackgroundSeedColor = prefs.getBoolean("use_background_seed_color", false)
        ThemeConfig.isHighContrastMode = prefs.getBoolean("high_contrast_mode", false)
    }

    private fun saveBackgroundUri(context: Context, uri: Uri?) {
        context.appPreferences.putString("custom_background", uri?.toString())
        context.appPreferences.putBoolean("prevent_background_refresh", false)
    }

    private fun resetBackgroundState(context: Context) {
        ThemeConfig.backgroundImageLoaded = false
        ThemeConfig.preventBackgroundRefresh = false
        context.appPreferences.putBoolean("prevent_background_refresh", false)
    }

    private fun copyImageToInternalStorage(context: Context, uri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "custom_background.jpg"
            val file = File(context.filesDir, fileName)

            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(8 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
            inputStream.close()

            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e(TAG, "复制图片失败: ${e.message}", e)
            null
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OpenJWCClientTheme(
    dpi: Int = 0,
    darkTheme: Boolean = isInDarkTheme(ThemeConfig.forceDarkMode),
    dynamicColor: Boolean = ThemeConfig.useDynamicColor,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemIsDark = isSystemInDarkTheme()

    // 初始化主题
    ThemeInitializer(context = context, systemIsDark = systemIsDark)

    // 创建颜色方案
    val colorScheme = createColorScheme(darkTheme, dynamicColor)

    val systemDensity = LocalDensity.current

    val density = remember(systemDensity, dpi) {
        if (dpi <= 0f) {
            systemDensity
        } else {
            val targetDensity = dpi / 160f
            Density(density = targetDensity, fontScale = systemDensity.fontScale)
        }
    }

    CompositionLocalProvider(
        LocalDensity provides density
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = ThemeConfig.motionScheme,
            typography = generateTypography()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                BackgroundLayer()
                content()
            }
        }
    }
}

@Composable
private fun ThemeInitializer(context: Context, systemIsDark: Boolean) {
    val themeChanged = ThemeConfig.detectThemeChange(systemIsDark)
    val scope = rememberCoroutineScope()

    // 处理系统主题变化
    LaunchedEffect(systemIsDark, themeChanged) {
        if (ThemeConfig.forceDarkMode == null && themeChanged) {
            Log.d("ThemeSystem", "系统主题变化: $systemIsDark")
            ThemeConfig.resetBackgroundState()

            if (!ThemeConfig.preventBackgroundRefresh) {
                BackgroundManager.loadCustomBackground(context)
            }

            CardConfig.apply {
                load(context)
                setThemeDefaults(systemIsDark)
                save(context)
            }
        }
    }

    // 初始加载配置
    LaunchedEffect(Unit) {
        scope.launch {
            ThemeManager.loadThemeMode(context)
            ThemeManager.loadSeedColor(context)
            ThemeManager.loadDynamicColorState(context)
            ThemeManager.loadDynamicColorSpec(context)
            ThemeManager.loadDynamicPaletteStyle(context)
            CardConfig.load(context)

            // 加载其余已保存的主题设置（保证持久化）
            val prefs = context.appPreferences
            ThemeConfig.predictiveBackAnimation = prefs.getString("predictive_back_animation", "AOSP") ?: "AOSP"
            ThemeConfig.predictiveBackExitDirection = prefs.getString("predictive_back_exit_direction", "FOLLOW_GESTURE") ?: "FOLLOW_GESTURE"
            ThemeConfig.animationMode = prefs.getString("animation_mode", ThemeConfig.ANIMATION_FULL) ?: ThemeConfig.ANIMATION_FULL
            ThemeConfig.isHighContrastMode = prefs.getBoolean("high_contrast_mode", false)
            ThemeConfig.isUseBackgroundSeedColor = prefs.getBoolean("use_background_seed_color", false)
            ThemeConfig.backgroundDim = prefs.getFloat("background_dim", 0f)

            if (!ThemeConfig.backgroundImageLoaded && !ThemeConfig.preventBackgroundRefresh) {
                BackgroundManager.loadCustomBackground(context)
            }
        }
    }

    MonetCompatInitializer(context)
}

@Composable
private fun MonetCompatInitializer(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return

    val scope = rememberCoroutineScope()

    DisposableEffect(context) {
        val monet = MonetCompat.setup(context)
        monet.defaultPrimaryColor = ThemeConfig.seedColor
        monet.defaultSecondaryColor = ThemeConfig.seedColor
        monet.defaultAccentColor = ThemeConfig.seedColor

        val listener = object : MonetColorsChangedListener {
            override fun onMonetColorsChanged(
                monet: MonetCompat,
                monetColors: MonetCompatColorScheme,
                isInitialChange: Boolean
            ) {
                scope.launch {
                    ThemeConfig.monetCompatSeedColor =
                        monet.getSelectedWallpaperColor() ?: ThemeConfig.seedColor
                }
            }
        }

        monet.addMonetColorsChangedListener(listener, notifySelf = true)
        onDispose {
            monet.removeMonetColorsChangedListener(listener)
        }
    }

    LaunchedEffect(ThemeConfig.useDynamicColor, ThemeConfig.seedColor) {
        if (!ThemeConfig.useDynamicColor) return@LaunchedEffect

        val monet = MonetCompat.setup(context)
        monet.defaultPrimaryColor = ThemeConfig.seedColor
        monet.defaultSecondaryColor = ThemeConfig.seedColor
        monet.defaultAccentColor = ThemeConfig.seedColor
        monet.updateConfiguration(context)
        ThemeConfig.monetCompatSeedColor =
            monet.getSelectedWallpaperColor() ?: ThemeConfig.seedColor
        monet.updateMonetColors()
    }
}

@Composable
private fun BackgroundLayer() {
    val context = LocalContext.current
    val backgroundUri = rememberSaveable { mutableStateOf(ThemeConfig.customBackgroundUri) }

    LaunchedEffect(ThemeConfig.customBackgroundUri) {
        backgroundUri.value = ThemeConfig.customBackgroundUri
        if (backgroundUri.value == null) {
            backgroundImagePainter = null
            backgroundSeedColor = 0
            context.appPreferences.remove("cached_seed_color")
        }
    }

    // 默认背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(-2f)
            .background(
                MaterialTheme.colorScheme.surfaceContainer
            )
    )

    // 自定义背景
    backgroundUri.value?.let { uri ->
        BackgroundInitializer(uri = uri)
    }
}

var backgroundImagePainter: AsyncImagePainter? by mutableStateOf(null)
var backgroundSeedColor by mutableIntStateOf(0)

private suspend fun Bitmap.extractSeedColor(
    maxColors: Int = 128,
    fallbackColorArgb: Int = -12417548
): Int = withContext(Dispatchers.IO) {
    val scaledBitmap = this@extractSeedColor.scale(128, 128)

    val width = scaledBitmap.width
    val height = scaledBitmap.height
    val pixels = IntArray(width * height)
    scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val colorToCountMap: Map<Int, Int> = QuantizerCelebi.quantize(pixels, maxColors)
    val sortedColors: List<Int> = Score.score(colorToCountMap, 10, fallbackColorArgb, true)

    if (scaledBitmap != this@extractSeedColor) {
        scaledBitmap.recycle()
    }

    sortedColors.firstOrNull() ?: fallbackColorArgb
}

@Composable
private fun BackgroundInitializer(uri: Uri) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val dynamicColorFromSystem =
        if (Build.VERSION.SDK_INT >= 31)
            colorResource(id = R.color.system_accent1_500).toArgb()
        else -12417548

    val calcedCachedSeedColor =
        context.appPreferences.getInt("cached_seed_color", dynamicColorFromSystem)

    backgroundImagePainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(uri)
            .build(),
        onError = { error ->
            Log.e("ThemeSystem", "背景加载失败: ${error.result.throwable.message}")
            ThemeConfig.customBackgroundUri = null
        },
        onSuccess = {
            Log.d("ThemeSystem", "背景加载成功")
            ThemeConfig.backgroundImageLoaded = true
            ThemeConfig.isThemeChanging = false

            val bitmap = it.result.image.toBitmap()

            backgroundSeedColor = calcedCachedSeedColor
            coroutineScope.launch {
                backgroundSeedColor = bitmap.extractSeedColor(
                    fallbackColorArgb = calcedCachedSeedColor
                )

                context.appPreferences.putInt("cached_seed_color", backgroundSeedColor)
            }
        }
    )
}

@Composable
private fun generateTypography(): androidx.compose.material3.Typography {
    val darkMode = isInDarkTheme(ThemeConfig.forceDarkMode)

    fun generateShadow(originalShadow: Shadow?): Shadow? {
        if (!ThemeConfig.isHighContrastMode) return originalShadow
        val shadow = originalShadow ?: Shadow(
            offset = Offset(2f, 3f),
            blurRadius = 2f
        )
        return shadow.copy(
            color = if (darkMode) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f)
        )
    }

    fun TextStyle.applyShadow() = this.copy(shadow = generateShadow(shadow))

    return remember(ThemeConfig.isHighContrastMode, darkMode) {
        val typography = Typography
        typography.copy(
        displayLarge = typography.displayLarge.applyShadow(),
        displayMedium = typography.displayMedium.applyShadow(),
        displaySmall = typography.displaySmall.applyShadow(),
        headlineLarge = typography.headlineLarge.applyShadow(),
        headlineMedium = typography.headlineMedium.applyShadow(),
        headlineSmall = typography.headlineSmall.applyShadow(),
        titleLarge = typography.titleLarge.applyShadow(),
        titleMedium = typography.titleMedium.applyShadow(),
        titleSmall = typography.titleSmall.applyShadow(),
        bodyLarge = typography.bodyLarge.applyShadow(),
        bodyMedium = typography.bodyMedium.applyShadow(),
        bodySmall = typography.bodySmall.applyShadow(),
        labelLarge = typography.labelLarge.applyShadow(),
        labelMedium = typography.labelMedium.applyShadow(),
        labelSmall = typography.labelSmall.applyShadow(),
        displayLargeEmphasized = typography.displayLargeEmphasized.applyShadow(),
        displayMediumEmphasized = typography.displayMediumEmphasized.applyShadow(),
        displaySmallEmphasized = typography.displaySmallEmphasized.applyShadow(),
        headlineLargeEmphasized = typography.headlineLargeEmphasized.applyShadow(),
        headlineMediumEmphasized = typography.headlineMediumEmphasized.applyShadow(),
        headlineSmallEmphasized = typography.headlineSmallEmphasized.applyShadow(),
        titleLargeEmphasized = typography.titleLargeEmphasized.applyShadow(),
        titleMediumEmphasized = typography.titleMediumEmphasized.applyShadow(),
        titleSmallEmphasized = typography.titleSmallEmphasized.applyShadow(),
        bodyLargeEmphasized = typography.bodyLargeEmphasized.applyShadow(),
        bodyMediumEmphasized = typography.bodyMediumEmphasized.applyShadow(),
        bodySmallEmphasized = typography.bodySmallEmphasized.applyShadow(),
        labelLargeEmphasized = typography.labelLargeEmphasized.applyShadow(),
        labelMediumEmphasized = typography.labelMediumEmphasized.applyShadow(),
        labelSmallEmphasized = typography.labelSmallEmphasized.applyShadow(),
    )
    }
}

@Composable
private fun createColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean
): ColorScheme {
    val seedColor =
        when {
            dynamicColor && ThemeConfig.isUseBackgroundSeedColor && backgroundSeedColor != 0 -> {
                backgroundSeedColor
            }

            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                colorResource(id = R.color.system_accent1_500).toArgb()
            }

            dynamicColor -> {
                ThemeConfig.monetCompatSeedColor
            }

            else -> {
                ThemeConfig.seedColor
            }
        }

    return dynamicColorScheme(
        seedColor = Color(seedColor),
        isDark = darkTheme,
        style = ThemeConfig.dynamicPaletteStyle,
        specVersion = ThemeConfig.dynamicColorSpec,
    )
}

@Composable
private fun SystemBarController(darkMode: Boolean) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
            ) { darkMode },
            navigationBarStyle = if (darkMode) {
                SystemBarStyle.dark(Color.Transparent.toArgb())
            } else {
                SystemBarStyle.light(
                    Color.Transparent.toArgb(),
                    Color.Transparent.toArgb()
                )
            }
        )
    }
}

// 向后兼容
@OptIn(DelicateCoroutinesApi::class)
fun Context.saveAndApplyCustomBackground(
    uri: Uri
) {
    GlobalScope.launch {
        BackgroundManager.saveAndApplyCustomBackground(
            this@saveAndApplyCustomBackground,
            uri
        )
    }
}

fun Context.saveCustomBackground(uri: Uri?) {
    if (uri != null) {
        saveAndApplyCustomBackground(uri)
    } else {
        BackgroundManager.clearCustomBackground(this)
    }
}

fun Context.saveThemeMode(forceDark: Boolean?) {
    ThemeManager.saveThemeMode(this, forceDark)
}


fun Context.saveThemeSeedColor(seedColor: Int) {
    ThemeManager.saveSeedColor(this, seedColor)
}


fun Context.saveDynamicColorState(enabled: Boolean) {
    ThemeManager.saveDynamicColorState(this, enabled)
}

fun Context.saveDynamicColorSpec(spec: ColorSpec.SpecVersion) {
    ThemeManager.saveDynamicColorSpec(this, spec)
}

fun Context.saveDynamicPaletteStyle(style: PaletteStyle) {
    ThemeManager.saveDynamicPaletteStyle(this, style)
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(themeMode: Boolean?): Boolean {
    return when (themeMode) {
        true -> true // 强制深色
        false -> false // 强制浅色
        null -> isSystemInDarkTheme() // 跟随系统
    }
}
