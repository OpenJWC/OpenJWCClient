package org.openjwc.client.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.datastore.CachedHitokoto
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.MenuItem
import org.openjwc.client.data.settings.SettingSection
import org.openjwc.client.navigation.Screen
import org.openjwc.client.net.models.NetworkResult
import java.time.LocalDate

class MeViewModel(
    private val repository: SettingsRepository,
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

    var hitokoto = repository.hitokotoFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CachedHitokoto()
    )

    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set

    fun refreshHitokotoLazily() {
        viewModelScope.launch {
            if (hitokoto.value.date != LocalDate.now()) {
                repository.tryRefreshHitokoto()
            }
        }
    }

    fun refreshHitokoto() {
        viewModelScope.launch {
            val result = repository.tryRefreshHitokoto()
            when (result) {
                is NetworkResult.Failure -> uiEvent.send(UiEvent.ShowToast("(${result.code}) ${result.msg}"))
                is NetworkResult.Error -> uiEvent.send(UiEvent.ShowToast(result.msg))
                is NetworkResult.Success -> uiEvent.send(UiEvent.ShowToast("刷新成功"))
            }
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