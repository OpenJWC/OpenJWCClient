package org.openjwc.client.data.settings

import androidx.compose.ui.graphics.vector.ImageVector
import org.openjwc.client.navigation.Screen

sealed class MenuItem {
    data class Route(
        val icon: ImageVector,
        val title: String? = null,
        val subtitle: String? = null,
        val route: Screen,
        val trailing: String? = null,
    ) : MenuItem()

    data class Action(
        val icon: ImageVector,
        val label: String? = null,
        val subtitle: String? = null,
        val trailing: String? = null,
        val onClick: () -> Unit
    ) : MenuItem()

    data class Toggle(
        val id: ToggleID,
        val icon: ImageVector,
        val label: String? = null,
        val isChecked: Boolean,
        val subtitle: String? = null,
    ) : MenuItem()
}

data class SettingSection(
    val title: String? = null,
    val items: List<MenuItem>
)

data class Menu(
    val route: Screen, val title: String, val sections: List<SettingSection>
)

enum class ToggleID {
    TEST_TOGGLE
}

sealed class Event {
    data class Toggle(val id: ToggleID) : Event()
    data class Route(val route: Screen) : Event()
    data class Action(val onAction: () -> Unit) : Event()
}
