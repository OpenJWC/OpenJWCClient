package org.openjwc.client.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.openjwc.client.settings.Menu
import org.openjwc.client.settings.MenuItem
import org.openjwc.client.settings.SettingSection
import org.openjwc.client.settings.ToggleID
import kotlin.collections.mapOf

data class UiState(
    val showXXXDialog: Boolean = false
    // TODO: 如果有弹窗，就写在这里
)

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    // 这里的 uiState 交给 SettingsScreen 去监听，拿个 LaunchedEffect 去弹框吧

    // TODO: 设置里的每一个 Toggle 都得让 ViewModel 来保存状态。

    private val _toggleState = MutableStateFlow(
        mapOf(
            ToggleID.TEST_TOGGLE to false
        )
    )
    private val menuTemplates =
        listOf(
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
                        title = "测试", items = listOf(
                            MenuItem.Route(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                route = "test",
                                title = "测试菜单",
                            )
                        )
                    )
                )
            ),

            Menu(
                route = "test",
                title = "测试菜单",
                sections = listOf(
                    SettingSection(
                        title = "测试类别", items = listOf(
                            MenuItem.Action(
                                icon = Icons.Filled.Settings,
                                label = "测试动作",
                                trailing = "测试尾部",
                                onClick = {}
                            ),
                            MenuItem.Toggle(
                                id = ToggleID.TEST_TOGGLE,
                                icon = Icons.Filled.Settings,
                                label = "测试开关",
                                isChecked = false,
                            )
                        )
                    )
                )
            )
        )

    val menus = _toggleState.map { states ->
        menuTemplates.map { menu ->
            menu.copy(
                sections = menu.sections.map { section ->
                    section.copy(
                        items = section.items.map { item ->
                            if (item is MenuItem.Toggle) {
                                // 注入实时状态
                                item.copy(isChecked = states[item.id] ?: item.isChecked)
                            } else item
                        }
                    )
                }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggle(id: ToggleID) {
        _toggleState.update { current ->
            current.toMutableMap().apply {
                this[id] = !(this[id] ?: false)
            }
        }
    }

}