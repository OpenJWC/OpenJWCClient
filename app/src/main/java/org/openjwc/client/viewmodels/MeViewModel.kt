package org.openjwc.client.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openjwc.client.data.settings.MenuItem
import org.openjwc.client.data.settings.SettingSection

class MeViewModel : ViewModel() {
    private val _sections = MutableStateFlow(
        listOf(
            SettingSection(
                title = null,
                items = listOf(
                    MenuItem.Route(
                        icon = Icons.Default.Settings,
                        route = "",
                        title = "设置",
                    ),
                    MenuItem.Route(
                        icon = Icons.Default.Info,
                        title = "关于",
                        route = "about"
                    )
                )
            ),
        )
    )
    val sections = _sections.asStateFlow()
}