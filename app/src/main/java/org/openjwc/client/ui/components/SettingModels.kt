package org.openjwc.client.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

sealed class SettingItem {
    data class Action(
        val icon: ImageVector,
        val label: String,
        val trailing: String? = null,
        val onClick: () -> Unit
    ) : SettingItem()

    data class Toggle(
        val icon: ImageVector,
        val label: String,
        val isChecked: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : SettingItem()
}

data class SettingSection(
    val title: String? = null,
    val items: List<SettingItem>
)