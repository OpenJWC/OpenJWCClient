package org.openjwc.client.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.MenuItem
import org.openjwc.client.data.settings.SettingSection
import org.openjwc.client.navigation.Screen
import org.openjwc.client.net.models.Hitokoto

class MeViewModel(
    private val repository: SettingsRepository
) : ViewModel() {
    private val tag = "MeViewModel"
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

    val defaultHitokoto =
        Hitokoto(
            text = "所谓觉悟，就是在漆黑的荒野中，开辟出一条理所应当前进的光明大道。",
            author = "乔鲁诺·乔巴纳"
        )

    var hitokoto = repository.userSettings.map { it.hitokoto }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = defaultHitokoto
    )
    fun refreshHitokoto() {
        viewModelScope.launch {
            repository.tryRefreshHitokoto()
        }
    }
}


class MeViewModelFactory(
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}