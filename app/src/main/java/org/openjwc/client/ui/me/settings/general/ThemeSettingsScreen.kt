package org.openjwc.client.ui.me.settings.general

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BlurOn
import androidx.compose.material.icons.twotone.Colorize
import androidx.compose.material.icons.twotone.Contrast
import androidx.compose.material.icons.twotone.OpenInFull
import androidx.compose.material.icons.twotone.Swipe
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.data.appPreferences
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsChooseWidget
import org.openjwc.client.ui.component.settings.SettingsSwitchWidget
import org.openjwc.client.ui.theme.BackgroundManager
import org.openjwc.client.ui.theme.ThemeConfig

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeSettingsScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.advanced_theme)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        ThemeSettingsContent(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        )
    }
}

@Composable
fun ThemeSettingsContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        SegmentedColumn(title = stringResource(R.string.accessibility)) {
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.Contrast,
                    title = stringResource(R.string.high_contrast_mode),
                    checked = ThemeConfig.isHighContrastMode,
                    onCheckedChange = { BackgroundManager.saveEnableHighContrastMode(context, it) }
                )
            }
        }

        SegmentedColumn(title = stringResource(R.string.predictive_back_animation)) {
            item {
                SettingsChooseWidget(
                    icon = Icons.TwoTone.Swipe,
                    title = stringResource(R.string.animation_type),
                    items = listOf("AOSP", "Scale", "KernelSU Classic", "MIUIX", "None"),
                    selectedIndex = when (ThemeConfig.predictiveBackAnimation) {
                        "AOSP" -> 0
                        "Scale" -> 1
                        "KernelSUClassic" -> 2
                        "MIUIX" -> 3
                        else -> 4
                    },
                    onSelectedIndexChange = { index ->
                        val value = listOf("AOSP", "Scale", "KernelSUClassic", "MIUIX", "None")[index]
                        context.appPreferences.putString("predictive_back_animation", value)
                        ThemeConfig.predictiveBackAnimation = value
                    }
                )
            }
            item {
                SettingsChooseWidget(
                    icon = Icons.TwoTone.OpenInFull,
                    title = stringResource(R.string.exit_direction),
                    items = listOf("FOLLOW_GESTURE", "ALWAYS_RIGHT", "ALWAYS_LEFT"),
                    selectedIndex = when (ThemeConfig.predictiveBackExitDirection) {
                        "FOLLOW_GESTURE" -> 0
                        "ALWAYS_RIGHT" -> 1
                        else -> 2
                    },
                    onSelectedIndexChange = { index ->
                        val value = listOf("FOLLOW_GESTURE", "ALWAYS_RIGHT", "ALWAYS_LEFT")[index]
                        context.appPreferences.putString("predictive_back_exit_direction", value)
                        ThemeConfig.predictiveBackExitDirection = value
                    }
                )
            }
        }
    }
}

