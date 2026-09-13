package org.openjwc.client.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BatterySaver
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openjwc.client.R
import org.openjwc.client.utils.areNotificationsEnabled
import org.openjwc.client.utils.isIgnoringBatteryOptimizations
import org.openjwc.client.utils.openBatteryOptimizationSettings
import org.openjwc.client.utils.openNotificationSettings
import org.openjwc.client.viewmodels.SettingsViewModel

private val WarningContainer = Color(0xFFFFC107)
private val WarningContent = Color(0xFF3E2723)

/**
 * 当通知或电池优化权限缺失时，在课程表主 FAB 上方显示的黄色警告 FAB。
 * 点击后弹出引导对话框；勾选“不再提醒”后永久隐藏。
 */
@Composable
fun PermissionReminderFab(
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    var notificationsEnabled by remember { mutableStateOf(areNotificationsEnabled(context)) }
    var batteryIgnored by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var showDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = areNotificationsEnabled(context)
                batteryIgnored = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val needsAttention = !settings.permissionReminderDismissed &&
        (!notificationsEnabled || !batteryIgnored)
    if (!needsAttention) return

    FloatingActionButton(
        modifier = modifier,
        containerColor = WarningContainer,
        contentColor = WarningContent,
        onClick = { showDialog = true }
    ) {
        Icon(
            imageVector = Icons.TwoTone.Warning,
            contentDescription = stringResource(R.string.permission_reminder_title)
        )
    }

    if (showDialog) {
        PermissionReminderDialog(
            notificationsEnabled = notificationsEnabled,
            batteryIgnored = batteryIgnored,
            onOpenNotification = { openNotificationSettings(context) },
            onOpenBattery = { openBatteryOptimizationSettings(context) },
            onDontRemind = { settingsViewModel.updatePermissionReminderDismissed(true) },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun PermissionReminderDialog(
    notificationsEnabled: Boolean,
    batteryIgnored: Boolean,
    onOpenNotification: () -> Unit,
    onOpenBattery: () -> Unit,
    onDontRemind: () -> Unit,
    onDismiss: () -> Unit
) {
    var dontRemind by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.TwoTone.Warning,
                contentDescription = null,
                tint = WarningContainer
            )
        },
        title = { Text(stringResource(R.string.permission_reminder_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.permission_reminder_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PermissionActionRow(
                    icon = Icons.TwoTone.Notifications,
                    title = stringResource(R.string.permission_reminder_notification),
                    granted = notificationsEnabled,
                    onClick = onOpenNotification
                )
                PermissionActionRow(
                    icon = Icons.TwoTone.BatterySaver,
                    title = stringResource(R.string.permission_reminder_battery),
                    granted = batteryIgnored,
                    onClick = onOpenBattery
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { dontRemind = !dontRemind }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = dontRemind, onCheckedChange = { dontRemind = it })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dont_remind_again),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (dontRemind) onDontRemind()
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        }
    )
}

@Composable
private fun PermissionActionRow(
    icon: ImageVector,
    title: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(
                    if (granted) R.string.permission_reminder_granted
                    else R.string.permission_reminder_not_granted
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
        Icon(
            imageVector = Icons.TwoTone.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
