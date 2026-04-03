package org.openjwc.client.data.settings

import Screen
import androidx.compose.ui.graphics.vector.ImageVector
import org.openjwc.client.net.models.Proxy
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle

sealed class MenuItem {
    data class Route(
        val icon: ImageVector,
        val title: String,
        val subtitle: String? = null,
        val route: Screen,
        val trailing: String? = null,
    ) : MenuItem()

    data class Action(
        val icon: ImageVector,
        val label: String,
        val subtitle: String? = null,
        val trailing: String? = null,
        val onClick: () -> Unit
    ) : MenuItem()

    data class Toggle(
        val id: ToggleID,
        val icon: ImageVector,
        val label: String,
        val isChecked: Boolean,
        val subtitle: String? = null,
    ) : MenuItem()
}

data class SettingSection(
    val title: String? = null, val items: List<MenuItem>
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

data class UserSettings(
    val policyAgreed: Boolean = false,
    val uuidString: String = "",
    val authKey: String = "",
    val themeStyle: DarkThemeStyle = DarkThemeStyle.Auto,
    val themeColor: ColorType = ColorType.Dynamic,
    val host: String = "101.132.106.186",
    val port: Int = 8000,
    val useHttp: Boolean = false,
    val freshDays: Int = 21,
    val backgroundPath: String? = null,
    val backgroundAlpha: Float = 0.3f,
    val proxy: Proxy = Proxy.NoProxy()
)