package org.openjwc.client.ui.me.settings.general

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AutoAwesome
import androidx.compose.material.icons.twotone.BlurOn
import androidx.compose.material.icons.twotone.Brightness6
import androidx.compose.material.icons.twotone.FormatPaint
import androidx.compose.material.icons.twotone.Palette
import androidx.compose.material.icons.twotone.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.navigation.Screen
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsBaseWidget
import org.openjwc.client.ui.component.settings.SettingsChooseWidget
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget
import org.openjwc.client.ui.component.settings.SettingsSwitchWidget
import org.openjwc.client.ui.theme.BackgroundManager
import org.openjwc.client.ui.theme.ColorItem
import org.openjwc.client.ui.theme.ThemeConfig
import org.openjwc.client.ui.theme.ThemeManager
import org.openjwc.client.ui.theme.ThemeSeedColors
import org.openjwc.client.ui.theme.blurEffect
import org.openjwc.client.ui.theme.blurSource
import org.openjwc.client.ui.theme.saveAndApplyCustomBackground

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { context.saveAndApplyCustomBackground(it) }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.blurEffect(),
                title = { Text(stringResource(R.string.theme)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
                .blurSource()
        ) {
            SegmentedColumn(title = stringResource(R.string.display_mode)) {
                item {
                    SettingsChooseWidget(
                        icon = Icons.TwoTone.Brightness6,
                        title = stringResource(R.string.display_mode),
                        items = listOf(stringResource(R.string.follow_system), stringResource(R.string.light), stringResource(R.string.dark)),
                        selectedIndex = when (ThemeConfig.forceDarkMode) { null -> 0; false -> 1; true -> 2 },
                        onSelectedIndexChange = { ThemeManager.saveThemeMode(context, when (it) { 0 -> null; 1 -> false; else -> true }) }
                    )
                }
            }

            SegmentedColumn(title = stringResource(R.string.color_theme)) {
                item {
                    SettingsSwitchWidget(
                        icon = Icons.TwoTone.AutoAwesome,
                        title = stringResource(R.string.dynamic_color),
                        checked = ThemeConfig.useDynamicColor,
                        onCheckedChange = { ThemeManager.saveDynamicColorState(context, it) }
                    )
                }
            }

            if (!ThemeConfig.useDynamicColor) {
                SegmentedColumn(title = stringResource(R.string.seed_color)) {
                    item {
                        SettingsBaseWidget(
                            icon = Icons.TwoTone.Palette,
                            title = stringResource(R.string.select_seed_color),
                        ) {}
                    }
                    item {
                        SettingsBaseWidget(
                            iconPlaceholder = false,
                            title = null,
                            foreContent = {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    ThemeSeedColors.all.forEach { color ->
                                        ColorItem(
                                            color = color,
                                            isSelected = color.toArgb() == ThemeConfig.seedColor,
                                            onClick = { ThemeManager.saveSeedColor(context, color.toArgb()) }
                                        )
                                    }
                                }
                            }
                        ) {}
                    }
                }
            }

            SegmentedColumn(title = stringResource(R.string.background)) {
                item {
                    SettingsBaseWidget(
                        icon = Icons.TwoTone.Wallpaper,
                        title = stringResource(R.string.custom_background),
                        onClick = { imagePicker.launch("image/*") }
                    ) {
                        Icon(Icons.TwoTone.FormatPaint, null, modifier = Modifier.size(24.dp))
                    }
                }
                if (ThemeConfig.customBackgroundUri != null) {
                    item {
                        SettingsBaseWidget(
                            icon = Icons.TwoTone.Wallpaper,
                            title = "Clear Background",
                            onClick = { BackgroundManager.clearCustomBackground(context) }
                        ) {}
                    }
                }
                item {
                    SettingsSwitchWidget(
                        icon = Icons.TwoTone.BlurOn,
                        title = "Enable Blur",
                        checked = ThemeConfig.isEnableBlur,
                        onCheckedChange = { BackgroundManager.saveEnableBlur(context, it) }
                    )
                }
                item {
                    SettingsSwitchWidget(
                        icon = Icons.TwoTone.BlurOn,
                        title = "Experimental Blur",
                        checked = ThemeConfig.isEnableBlurExp,
                        onCheckedChange = { BackgroundManager.saveEnableBlurExp(context, it) }
                    )
                }
            }

            SegmentedColumn {
                item {
                    SettingsJumpPageWidget(
                        icon = Icons.TwoTone.Palette,
                        title = stringResource(R.string.advanced_theme),
                        onClick = { navigator.push(Screen.ThemeSettings) }
                    )
                }
            }
        }
    }
}
