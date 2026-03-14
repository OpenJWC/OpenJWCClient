package org.openjwc.client.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.ui.main.MainTab
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle

class MainViewModel(
    private val repository: SettingsRepository
) : ViewModel() {
    private val label = "MainViewModel"
    private val _currentTab = MutableStateFlow<MainTab>(MainTab.Chat)
    val currentTab = _currentTab.asStateFlow()

    fun updateTab(tab: MainTab) {
        _currentTab.value = tab
        Log.d(label, "Update tab to $tab")

    }

    val settings: StateFlow<UserSettings> = repository.userSettings
        .map { it ?: UserSettings() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    init {
        viewModelScope.launch {
            settings.collect { newSettings ->
                Log.d(label, "数据库已同步新状态: $newSettings")
            }
        }
    }
    // 假设你从 repository 获取 flow
    val themeColor: StateFlow<ColorType> = repository.userSettings
        .map { (it ?: UserSettings()).themeColor }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ColorType.Dynamic
        )

    val darkThemeStyle: StateFlow<DarkThemeStyle> = repository.userSettings
        .map { (it ?: UserSettings()).themeStyle }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DarkThemeStyle.Auto
        )


    fun updateThemeColor(color: ColorType) {
        Log.d(label, "Update theme color to $color")
        viewModelScope.launch {
            repository.updateThemeColor(color)
        }
    }

    fun updateDarkThemeStyle(style: DarkThemeStyle) {
        Log.d(label, "Update dark theme style to $style")
        viewModelScope.launch {
            repository.updateThemeStyle(style)
        }
    }
}

class MainViewModelFactory(
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // 💡 传入两个 Repository
            return MainViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}