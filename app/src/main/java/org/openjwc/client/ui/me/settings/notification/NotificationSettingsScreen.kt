package org.openjwc.client.ui.me.settings.notification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material.icons.twotone.Schedule
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openjwc.client.R
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsDropdownWidget
import org.openjwc.client.ui.component.settings.SettingsSwitchWidget
import org.openjwc.client.viewmodels.SettingsViewModel
import org.openjwc.client.work.NewsNotificationScheduler

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationSettingsScreen(navigator: Navigator, settingsViewModel: SettingsViewModel) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.notification_settings)) },
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
        NotificationContent(
            settingsViewModel = settingsViewModel,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
        )
    }
}

@Composable
fun NotificationContent(settingsViewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    val intervalChoices = listOf(15, 30, 60, 180, 360)
    val intervalLabels = listOf(
        stringResource(R.string.interval_15m),
        stringResource(R.string.interval_30m),
        stringResource(R.string.interval_1h),
        stringResource(R.string.interval_3h),
        stringResource(R.string.interval_6h)
    )
    val selectedIntervalIndex = intervalChoices.indexOf(settings.newsCheckIntervalMinutes)
        .coerceAtLeast(0)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        SegmentedColumn(title = stringResource(R.string.news_notification)) {
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.Notifications,
                    title = stringResource(R.string.news_notification),
                    description = stringResource(R.string.news_notification_desc),
                    checked = settings.newsNotificationEnabled,
                    onCheckedChange = { checked ->
                        settingsViewModel.updateNewsNotificationEnabled(checked)
                        if (checked) {
                            NewsNotificationScheduler.runOnce(context)
                        }
                    }
                )
            }
            item {
                SettingsDropdownWidget(
                    icon = Icons.TwoTone.Schedule,
                    title = stringResource(R.string.news_check_interval),
                    enabled = settings.newsNotificationEnabled,
                    choice = selectedIntervalIndex,
                    data = intervalLabels,
                    trailingContent = {
                        Text(
                            text = intervalLabels.getOrNull(selectedIntervalIndex) ?: "",
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = if (settings.newsNotificationEnabled) 1f else 0.38f)
                        )
                    },
                    onChoiceChange = { index ->
                        intervalChoices.getOrNull(index)?.let {
                            settingsViewModel.updateNewsCheckIntervalMinutes(it)
                        }
                    }
                )
            }
        }
    }
}
