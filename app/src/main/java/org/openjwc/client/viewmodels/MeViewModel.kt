package org.openjwc.client.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
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
                title = "投稿",
                items = listOf(
                    MenuItem.Route(
                        icon = Icons.Default.Add,
                        title = "投稿资讯",
                        route = Screen.UploadNews,
                    ),
                    MenuItem.Route(
                        icon = Icons.Default.Search,
                        title = "查看投稿审核结果",
                        route = Screen.Review,
                    )
                )
            ),
            SettingSection(
                title = null,
                items = listOf(
                    MenuItem.Route(
                        icon = Icons.Default.Settings,
                        route = Screen.Settings,
                        title = "设置",
                    ),
                    MenuItem.Route(
                        icon = Icons.Default.Info,
                        title = "关于",
                        route = Screen.About
                    ),
                )
            )
        )
    )
    val sections = _sections.asStateFlow()
}