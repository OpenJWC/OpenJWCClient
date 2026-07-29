package org.openjwc.client.ui.me.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BugReport
import androidx.compose.material.icons.twotone.CalendarMonth
import androidx.compose.material.icons.twotone.Dns
import androidx.compose.material.icons.twotone.Language
import androidx.compose.material.icons.twotone.Newspaper
import androidx.compose.material.icons.twotone.Palette
import androidx.compose.material.icons.twotone.VpnKey
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget
import org.openjwc.client.ui.me.settings.auth.AccountContent
import org.openjwc.client.ui.me.settings.auth.AccountScreen
import org.openjwc.client.ui.me.settings.connection.HostContent
import org.openjwc.client.ui.me.settings.connection.HostScreen
import org.openjwc.client.ui.me.settings.general.LanguageContent
import org.openjwc.client.ui.me.settings.general.LanguageScreen
import org.openjwc.client.ui.me.settings.general.ThemeContent
import org.openjwc.client.ui.me.settings.general.ThemeScreen
import org.openjwc.client.ui.me.settings.log.LogContent
import org.openjwc.client.ui.me.settings.log.LogScreen
import org.openjwc.client.ui.me.settings.news.NewsContent
import org.openjwc.client.ui.me.settings.news.NewsDisplaySettingsScreen
import org.openjwc.client.ui.me.settings.timetable.TimetableContent
import org.openjwc.client.ui.me.settings.timetable.TimetablePrefsScreen
import org.openjwc.client.viewmodels.AuthViewModel
import org.openjwc.client.viewmodels.NewsViewModel
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.viewmodels.TimetableViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SettingsScreen(
    navigator: Navigator,
    settingsViewModel: SettingsViewModel? = null,
    authViewModel: AuthViewModel? = null,
    newsViewModel: NewsViewModel? = null,
    timetableViewModel: TimetableViewModel? = null
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as android.app.Activity)
    val isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    var selectedPage by remember { mutableStateOf<Screen?>(null) }

    if (isWide && settingsViewModel != null) {
        Column(Modifier.fillMaxSize()) {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = MaterialTheme.colorScheme.surface)
            )
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.width(300.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                    SegmentedColumn(title = stringResource(R.string.general)) {
                        item { SettingsBaseWidget(icon = Icons.TwoTone.Palette, title = stringResource(R.string.theme), selected = selectedPage == Screen.Theme, onClick = { selectedPage = Screen.Theme }) {} }
                        item { SettingsBaseWidget(icon = Icons.TwoTone.Language, title = stringResource(R.string.language), selected = selectedPage == Screen.Language, onClick = { selectedPage = Screen.Language }) {} }
                    }
                    SegmentedColumn(title = stringResource(R.string.connection)) {
                        item { SettingsBaseWidget(icon = Icons.TwoTone.Dns, title = stringResource(R.string.network_config), selected = selectedPage == Screen.Host, onClick = { selectedPage = Screen.Host }) {} }
                        item { SettingsBaseWidget(icon = Icons.TwoTone.VpnKey, title = stringResource(R.string.account_management), selected = selectedPage == Screen.Account, onClick = { selectedPage = Screen.Account }) {} }
                    }
                    SegmentedColumn(title = stringResource(R.string.news)) {
                        item { SettingsBaseWidget(icon = Icons.TwoTone.Newspaper, title = stringResource(R.string.display_settings), selected = selectedPage == Screen.NewsSettings, onClick = { selectedPage = Screen.NewsSettings }) {} }
                    }
                    SegmentedColumn(title = stringResource(R.string.timetable)) {
                        item { SettingsBaseWidget(icon = Icons.TwoTone.CalendarMonth, title = stringResource(R.string.timetable_settings), selected = selectedPage == Screen.TimetablePrefs, onClick = { selectedPage = Screen.TimetablePrefs }) {} }
                    }
                    SegmentedColumn(title = stringResource(R.string.debug)) {
                        item { SettingsBaseWidget(icon = Icons.TwoTone.BugReport, title = stringResource(R.string.log), selected = selectedPage == Screen.Log, onClick = { selectedPage = Screen.Log }) {} }
                    }
                }

                Box(Modifier.weight(1f).fillMaxHeight()) {
                    val contentModifier = Modifier.fillMaxSize().padding(16.dp)
                    when (selectedPage) {
                        Screen.Theme -> ThemeContent(navigator, contentModifier)
                        Screen.Language -> LanguageContent(navigator, settingsViewModel, contentModifier)
                        Screen.Host -> HostContent(navigator, settingsViewModel, contentModifier)
                        Screen.Account -> AccountContent(navigator, authViewModel!!, settingsViewModel)
                        Screen.NewsSettings -> NewsContent(navigator, settingsViewModel, contentModifier)
                        Screen.TimetablePrefs -> TimetableContent(settingsViewModel, timetableViewModel!!, contentModifier)
                        Screen.Log -> LogContent(navigator, modifier = contentModifier)
                        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select a setting", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                        }
                        else -> {}
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    navigationIcon = { AppBackButton(onClick = { navigator.pop() }) },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState()).padding(innerPadding)) {
                SegmentedColumn(title = stringResource(R.string.general)) {
                    item { SettingsJumpPageWidget(icon = Icons.TwoTone.Palette, title = stringResource(R.string.theme), onClick = { navigator.push(Screen.Theme) }) }
                    item { SettingsJumpPageWidget(icon = Icons.TwoTone.Language, title = stringResource(R.string.language), onClick = { navigator.push(Screen.Language) }) }
                }
                SegmentedColumn(title = stringResource(R.string.connection)) {
                    item { SettingsJumpPageWidget(icon = Icons.TwoTone.Dns, title = stringResource(R.string.network_config), onClick = { navigator.push(Screen.Host) }) }
                    item { SettingsJumpPageWidget(icon = Icons.TwoTone.VpnKey, title = stringResource(R.string.account_management), onClick = { navigator.push(Screen.Account) }) }
                }
                SegmentedColumn(title = stringResource(R.string.news)) {
                    item { SettingsJumpPageWidget(icon = Icons.TwoTone.Newspaper, title = stringResource(R.string.display_settings), onClick = { navigator.push(Screen.NewsSettings) }) }
                }
                SegmentedColumn(title = stringResource(R.string.timetable)) {
                    item { SettingsJumpPageWidget(icon = Icons.TwoTone.CalendarMonth, title = stringResource(R.string.timetable_settings), onClick = { navigator.push(Screen.TimetablePrefs) }) }
                }
                SegmentedColumn(title = stringResource(R.string.debug)) {
                    item { SettingsJumpPageWidget(icon = Icons.TwoTone.BugReport, title = stringResource(R.string.log), onClick = { navigator.push(Screen.Log) }) }
                }
            }
        }
    }
}
