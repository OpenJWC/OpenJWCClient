package org.openjwc.client.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.openjwc.client.ui.general.SettingItem
import org.openjwc.client.ui.general.SettingSection


private val MOCK_SETTING_SECTION = listOf(
    SettingSection(
        title = "General",
        items = listOf(
            SettingItem.Action(
                icon = Icons.Default.Info,
                label = "About",
                onClick = {}
            ),
            SettingItem.Action(
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = {}
            ),
        )
    ),
)
class MeViewModel : ViewModel() {
    private val _sections = MutableStateFlow<List<SettingSection>>(MOCK_SETTING_SECTION)
    val sections = _sections.asStateFlow()
}