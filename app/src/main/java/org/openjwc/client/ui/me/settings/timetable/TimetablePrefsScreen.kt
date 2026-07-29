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
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openjwc.client.R
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.CachedDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.db.AppDatabase
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.data.repository.CourseRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsSwitchWidget
import org.openjwc.client.ui.theme.blurEffect
import org.openjwc.client.ui.theme.blurSource
import org.openjwc.client.viewmodels.TimetableViewModel
import org.openjwc.client.viewmodels.TimetableViewModelFactory
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.viewmodels.SettingsViewModelFactory

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimetablePrefsScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    val database = remember { AppDatabase.getDatabase(context) }
    val settingsDataSource = remember { SettingsDataSource(context) }
    val authDataSource = remember { AuthDataSource(context) }
    val cachedDataSource = remember { CachedDataSource(context) }
    val settingsRepository = remember { SettingsRepository(settingsDataSource, cachedDataSource, authDataSource, context) }
    val courseRepository = remember { CourseRepository(database.courseDao(), database.tableDao()) }
    val authRepository = remember { AuthRepository(authDataSource, settingsDataSource) }
    val timetableViewModel: TimetableViewModel = viewModel(factory = TimetableViewModelFactory(courseRepository, settingsRepository))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsRepository, authRepository))

    val prefs by timetableViewModel.displayPrefs.collectAsStateWithLifecycle()
    val showTimeline = prefs.showTimeline
    val showDate = prefs.showDate
    val showPeriodTime = prefs.showPeriodTime
    val showNonCurrentWeek = prefs.showNonCurrentWeek

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.blurEffect(),
                title = { Text(stringResource(R.string.timetable_settings)) },
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .blurSource()
        ) {
            SegmentedColumn {
                item {
                    SettingsSwitchWidget(
                        icon = Icons.TwoTone.Timeline,
                        title = stringResource(R.string.show_timeline),
                        description = stringResource(R.string.show_timeline_desc),
                        checked = showTimeline,
                        onCheckedChange = { settingsViewModel.updateShowTimeline(it) }
                    )
                }
                item {
                    SettingsSwitchWidget(
                        icon = Icons.TwoTone.CalendarMonth,
                        title = stringResource(R.string.show_date_header),
                        description = stringResource(R.string.show_date_header_desc),
                        checked = showDate,
                        onCheckedChange = { settingsViewModel.updateShowDate(it) }
                    )
                }
                item {
                    SettingsSwitchWidget(
                        icon = Icons.TwoTone.Schedule,
                        title = stringResource(R.string.show_period_time),
                        description = stringResource(R.string.show_period_time_desc),
                        checked = showPeriodTime,
                        onCheckedChange = { settingsViewModel.updateShowPeriodTime(it) }
                    )
                }
                item {
                    SettingsSwitchWidget(
                        icon = Icons.TwoTone.VisibilityOff,
                        title = stringResource(R.string.show_non_current_week),
                        description = stringResource(R.string.show_non_current_week_desc),
                        checked = showNonCurrentWeek,
                        onCheckedChange = { settingsViewModel.updateShowNonCurrentWeek(it) }
                    )
                }
            }
        }
    }
}
