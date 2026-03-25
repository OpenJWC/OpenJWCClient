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

data class MainUiState(
    val themeColor: ColorType = UserSettings().themeColor,
    val darkThemeStyle: DarkThemeStyle = UserSettings().themeStyle,
    val agreedPolicy: Boolean? = null,
    val backgroundPath: String? = null,
    val backgroundAlpha: Float = 1f,
    val isReady: Boolean = false
)

class MainViewModel(
    private val repository: SettingsRepository
) : ViewModel() {
    init {
        viewModelScope.launch {
            repository.getOrGenerateDeviceId()
        }
    }

    private val label = "MainViewModel"
    private val _currentTab = MutableStateFlow<MainTab>(MainTab.Chat)
    val currentTab = _currentTab.asStateFlow()
    fun updateTab(tab: MainTab) {
        _currentTab.value = tab
        Log.d(label, "Update tab to $tab")

    }

    val uiState: StateFlow<MainUiState> = repository.userSettings
        .map { settings ->
            MainUiState(
                themeColor = settings.themeColor,
                darkThemeStyle = settings.themeStyle,
                agreedPolicy = settings.policyAgreed,
                backgroundPath = settings.backgroundPath,
                backgroundAlpha = settings.backgroundAlpha,
                isReady = true
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MainUiState()
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

    fun agreePolicy() {
        Log.d(label, "Agree policy")
        viewModelScope.launch {
            repository.agreePolicy()
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