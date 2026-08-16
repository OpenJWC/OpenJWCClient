package org.openjwc.client.ui.timetable.edit.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.openjwc.client.ui.component.settings.SettingsBaseWidget

/**
 * MD3E 风格的课表操作条目，基于 [SettingsBaseWidget]。
 * 危险操作（isDanger=true）使用错误色。
 */
@Composable
fun ActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    SettingsBaseWidget(
        icon = icon,
        iconColor = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        title = title,
        titleStyle = MaterialTheme.typography.titleMedium.copy(
            color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        ),
        description = subtitle,
        descriptionColor = if (isDanger) MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = { onClick() }
    )
}
