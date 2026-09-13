package org.openjwc.client.ui.me.settings.notification

import android.Manifest
import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BatterySaver
import androidx.compose.material.icons.twotone.CalendarMonth
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openjwc.client.R
import org.openjwc.client.navigation3.Navigator
import org.openjwc.client.ui.component.settings.AppBackButton
import org.openjwc.client.ui.component.settings.SegmentedColumn
import org.openjwc.client.ui.component.settings.SettingsDropdownWidget
import org.openjwc.client.ui.component.settings.SettingsSwitchWidget
import org.openjwc.client.utils.isIgnoringBatteryOptimizations
import org.openjwc.client.utils.isNotificationPermissionGranted
import org.openjwc.client.utils.openBatteryOptimizationList
import org.openjwc.client.utils.openBatteryOptimizationSettings
import org.openjwc.client.utils.openNotificationSettings
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    var notificationGranted by remember {
        mutableStateOf(isNotificationPermissionGranted(context))
    }
    var batteryOptimizationIgnored by remember {
        mutableStateOf(isIgnoringBatteryOptimizations(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
        val activity = context as? Activity
        // 仅在“不再询问”导致系统不再弹窗时，才引导用户去系统设置手动开启
        if (!granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            openNotificationSettings(context)
        }
    }

    // 从系统设置返回时刷新权限状态
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationGranted = isNotificationPermissionGranted(context)
                batteryOptimizationIgnored = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
        SegmentedColumn(title = stringResource(R.string.system_permissions)) {
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.Notifications,
                    title = stringResource(R.string.notification_permission),
                    description = stringResource(R.string.notification_permission_summary),
                    checked = notificationGranted,
                    onCheckedChange = {
                        when {
                            notificationGranted -> openNotificationSettings(context)
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else -> openNotificationSettings(context)
                        }
                    }
                )
            }
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.BatterySaver,
                    title = stringResource(R.string.battery_optimization),
                    description = stringResource(R.string.battery_optimization_summary),
                    checked = batteryOptimizationIgnored,
                    onCheckedChange = {
                        val opened = if (batteryOptimizationIgnored) {
                            // 已忽略：打开电池优化列表，用户可改回“优化”以关闭
                            openBatteryOptimizationList(context)
                        } else {
                            openBatteryOptimizationSettings(context)
                        }
                        if (!opened) {
                            Toast.makeText(context, R.string.cannot_open_settings, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        SegmentedColumn(title = stringResource(R.string.news_notification)) {
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.Notifications,
                    title = stringResource(R.string.news_notification),
                    description = stringResource(R.string.news_notification_desc),
                    enabled = notificationGranted,
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

        SegmentedColumn(title = stringResource(R.string.course_reminder)) {
            item {
                SettingsSwitchWidget(
                    icon = Icons.TwoTone.CalendarMonth,
                    title = stringResource(R.string.course_reminder),
                    description = stringResource(R.string.course_reminder_desc),
                    enabled = notificationGranted,
                    checked = settings.courseReminderEnabled,
                    onCheckedChange = { checked ->
                        settingsViewModel.updateCourseReminderEnabled(checked)
                    }
                )
            }
        }
    }
}