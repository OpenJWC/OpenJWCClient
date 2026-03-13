package org.openjwc.client.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openjwc.client.settings.Event
import org.openjwc.client.settings.MenuItem
import org.openjwc.client.settings.SettingSection
import org.openjwc.client.settings.ToggleID

@Composable
fun MenuListItem(
    item: MenuItem, onEvent: (Event) -> Unit
) {
    when (item) {
        is MenuItem.Route -> {
            Surface(
                onClick = { onEvent(Event.Route(item.route)) }, color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    // Menu 通常固定显示向右箭头
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go to ${item.title}",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        is MenuItem.Action -> {
            Surface(
                onClick = { onEvent(Event.Action(item.onClick)) }, color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.trailing != null) {
                        Text(
                            text = item.trailing,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        is MenuItem.Toggle -> {
            Surface(
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = item.isChecked,
                        onCheckedChange = { onEvent(Event.Toggle(item.id)) }
                    )
                }
            }
        }
    }
}


@Composable
fun MenuSectionCard(
    section: SettingSection, onEvent: (Event) -> Unit
) {
    Column {
        if (section.title != null) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column {
                section.items.forEachIndexed { index, item ->
                    MenuListItem(item, onEvent)
                    if (index < section.items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun TestMenuListItem() {
    val s = MenuItem.Action(
        icon = Icons.Default.Info, label = "测试", onClick = {})
    MenuListItem(s) {}
}

@Preview
@Composable
fun TestMenuSectionCard() {
    val s1 = MenuItem.Action(
        icon = Icons.Default.Info, label = "测试动作", trailing = "测试尾部", onClick = {})

    val s2 = MenuItem.Toggle(
        id = ToggleID.TEST_TOGGLE,
        icon = Icons.Default.Info,
        label = "测试开关",
        isChecked = true
    )

    val s3 = MenuItem.Route(
        icon = Icons.Default.Info,
        title = "测试菜单",
        route = "",
    )

    val ss = SettingSection(
        title = "测试类别", items = listOf(s1, s2, s3)
    )
    MenuSectionCard(
        ss, onEvent = {})
}