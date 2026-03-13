package org.openjwc.client.settings

import androidx.compose.ui.graphics.vector.ImageVector

sealed class MenuItem {
    data class Route(
        val icon: ImageVector,
        val title: String,
        val route: String,
    ) : MenuItem()

    data class Action(
        val icon: ImageVector,
        val label: String,
        val trailing: String? = null,
        val onClick: () -> Unit
    ) : MenuItem()

    data class Toggle(
        val id: ToggleID,
        val icon: ImageVector,
        val label: String,
        val isChecked: Boolean,
    ) : MenuItem()
}

data class SettingSection(
    val title: String? = null,
    val items: List<MenuItem>
)

data class Menu(
    val route: String,
    val title: String,
    val sections: List<SettingSection>
)

enum class ToggleID {
    TEST_TOGGLE
}

sealed class Event {
    data class Toggle(val id: ToggleID) : Event()
    data class Route(val route: String) : Event()
    data class Action(val onAction: () -> Unit) : Event()
}