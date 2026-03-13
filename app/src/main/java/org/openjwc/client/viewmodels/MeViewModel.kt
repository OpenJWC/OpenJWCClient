package org.openjwc.client.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openjwc.client.navigation.me.Routes.buildSettingsRoute
import org.openjwc.client.settings.MenuItem
import org.openjwc.client.settings.SettingSection

class MeViewModel : ViewModel() {
    private val _sections = MutableStateFlow(
        listOf(
            SettingSection(
                title = null,
                items = listOf(
                    MenuItem.Route(
                        icon = Icons.Default.Settings,
                        route = buildSettingsRoute(""),
                        title = "设置",
                    ),
                    MenuItem.Action(
                        icon = Icons.Default.Info,
                        label = "关于",
                        onClick = {
                        }
                    )
                )
            ),
        )
    )
    val sections = _sections.asStateFlow()
}