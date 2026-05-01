package org.openjwc.client.ui.timetable.edit.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun ActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val subColor = if (isDanger) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        headlineContent = { Text(title, color = contentColor) },
        supportingContent = { Text(subtitle, color = subColor) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        modifier = Modifier.clickable { onClick() }
    )
}