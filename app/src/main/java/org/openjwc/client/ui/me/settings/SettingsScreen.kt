package org.openjwc.client.ui.me.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import org.openjwc.client.R
import org.openjwc.client.navigation.Screen
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsJumpPageWidget

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            SegmentedColumn(title = stringResource(R.string.general)) {
                item {
                    SettingsJumpPageWidget(
                        icon = Icons.TwoTone.Palette,
                        title = stringResource(R.string.theme),
                        onClick = { navigator.push(Screen.Theme) }
                    )
                }
                item {
                    SettingsJumpPageWidget(
                        icon = Icons.TwoTone.Language,
                        title = stringResource(R.string.language),
                        onClick = { navigator.push(Screen.Language) }
                    )
                }
            }

            SegmentedColumn(title = stringResource(R.string.connection)) {
                item {
                    SettingsJumpPageWidget(
                        icon = Icons.TwoTone.Dns,
                        title = stringResource(R.string.network_config),
                        onClick = { navigator.push(Screen.Host) }
                    )
                }
                item {
                    SettingsJumpPageWidget(
                        icon = Icons.TwoTone.VpnKey,
                        title = stringResource(R.string.account_management),
                        onClick = { navigator.push(Screen.Account) }
                    )
                }
            }

            SegmentedColumn(title = stringResource(R.string.news)) {
                item {
                    SettingsJumpPageWidget(
                        icon = Icons.TwoTone.Newspaper,
                        title = stringResource(R.string.display_settings),
                        onClick = { navigator.push(Screen.NewsSettings) }
                    )
                }
            }

            SegmentedColumn(title = stringResource(R.string.timetable)) {
                item {
                    SettingsJumpPageWidget(
                        icon = Icons.TwoTone.CalendarMonth,
                        title = stringResource(R.string.timetable_settings),
                        onClick = { navigator.push(Screen.TimetablePrefs) }
                    )
                }
            }

            SegmentedColumn(title = stringResource(R.string.debug)) {
                item {
                    SettingsJumpPageWidget(
                        icon = Icons.TwoTone.BugReport,
                        title = stringResource(R.string.log),
                        onClick = { navigator.push(Screen.Log) }
                    )
                }
            }
        }
    }
}
