package org.openjwc.client.data.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle

sealed class MenuItem {
    data class Route(
        val icon: ImageVector,
        val title: String,
        val route: String,
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
    val route: String, val title: String, val sections: List<SettingSection>
)

enum class ToggleID {
    TEST_TOGGLE
}

sealed class Event {
    data class Toggle(val id: ToggleID) : Event()
    data class Route(val route: String) : Event()
    data class Action(val onAction: () -> Unit) : Event()
}

val menuTemplates = listOf(
    Menu(
        route = "", title = "设置", sections = listOf(
            SettingSection(
                title = "通用", items = listOf(
                    MenuItem.Route(
                        icon = Icons.Default.Palette,
                        route = "theme",
                        title = "主题",
                    )
                )
            ), SettingSection(
                title = "连接", items = listOf(
                    MenuItem.Route(
                        icon = Icons.Default.Dns,
                        route = "host",
                        title = "服务器配置",
                    ),
                    MenuItem.Route(
                        icon = Icons.Default.VpnKey,
                        route = "auth",
                        title = "鉴权设置",
                    )
                )
            )
        )
    )
)

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