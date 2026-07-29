package org.openjwc.client.ui.me.settings.timetable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.CalendarMonth
import androidx.compose.material.icons.twotone.Schedule
import androidx.compose.material.icons.twotone.Timeline
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.openjwc.client.R
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsSwitchWidget
import org.openjwc.client.viewmodels.TimetableViewModel
import org.openjwc.client.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimetablePrefsScreen(navigator: Navigator, settingsViewModel: SettingsViewModel, timetableViewModel: TimetableViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.timetable_settings)) },
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
        TimetableContent(
            settingsViewModel = settingsViewModel,
            timetableViewModel = timetableViewModel,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        )
    }
}

@Composable
fun TimetableContent(
    settingsViewModel: SettingsViewModel,
    timetableViewModel: TimetableViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by timetableViewModel.displayPrefs.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        SegmentedColumn {
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.Timeline,
                    title = stringResource(R.string.show_timeline),
                    description = stringResource(R.string.show_timeline_desc),
                    checked = prefs.showTimeline,
                    onCheckedChange = { settingsViewModel.updateShowTimeline(it) }
                )
            }
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.CalendarMonth,
                    title = stringResource(R.string.show_date_header),
                    description = stringResource(R.string.show_date_header_desc),
                    checked = prefs.showDate,
                    onCheckedChange = { settingsViewModel.updateShowDate(it) }
                )
            }
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.Schedule,
                    title = stringResource(R.string.show_period_time),
                    description = stringResource(R.string.show_period_time_desc),
                    checked = prefs.showPeriodTime,
                    onCheckedChange = { settingsViewModel.updateShowPeriodTime(it) }
                )
            }
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.VisibilityOff,
                    title = stringResource(R.string.show_non_current_week),
                    description = stringResource(R.string.show_non_current_week_desc),
                    checked = prefs.showNonCurrentWeek,
                    onCheckedChange = { settingsViewModel.updateShowNonCurrentWeek(it) }
                )
            }
        }
    }
}

