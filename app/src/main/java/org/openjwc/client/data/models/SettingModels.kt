package org.openjwc.client.data.models

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

sealed interface SettingsItem {
    val icon: ImageVector
    val title: String
    val subtitle: String?

    data class Route(
        override val icon: ImageVector,
        override val title: String,
        override val subtitle: String? = null,
        val target: NavKey,
        val trailing: String? = null,
    ) : SettingsItem

    data class Action(
        override val icon: ImageVector,
        override val title: String,
        override val subtitle: String? = null,
        val trailing: String? = null,
        val onClick: () -> Unit
    ) : SettingsItem

    data class Toggle(
        override val icon: ImageVector,
        override val title: String,
        val isChecked: Boolean,
        override val subtitle: String? = null,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingsItem
}

data class SettingsSection(
    val title: String? = null,
    val items: List<SettingsItem>
)
