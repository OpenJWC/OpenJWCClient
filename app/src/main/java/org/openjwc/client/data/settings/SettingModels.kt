package org.openjwc.client.data.settings

import Screen
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle

sealed class MenuItem {
    data class Route(
        val icon: ImageVector,
        val title: String,
        val route: Screen,
        val trailing: String? = null,
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

// 这个是数据库里的实体
@Entity(tableName = "settings")
data class UserSettings(
    @PrimaryKey val id: Int = 0,
    val uuidString: String = "",
    val authKey: String = "",
    val themeStyle: DarkThemeStyle = DarkThemeStyle.Auto,
    val themeColor: ColorType = ColorType.Dynamic,
    val host: String = "101.132.106.186",
    val port: Int = 8000,
)