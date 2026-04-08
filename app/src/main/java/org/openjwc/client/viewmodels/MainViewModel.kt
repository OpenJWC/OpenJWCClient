package org.openjwc.client.viewmodels

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
import org.openjwc.client.BuildConfig
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.log.Logger
import org.openjwc.client.net.update.getLatestRelease
import org.openjwc.client.net.models.CheckUpdateClient
import org.openjwc.client.net.models.GitHubRelease
import org.openjwc.client.net.models.NetworkResult
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
        Logger.d(label, "Update tab to $tab")

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

    var updateRelease = MutableStateFlow<GitHubRelease?>(null)
        private set

    var updateDismissed = MutableStateFlow(false)
        private set


    fun updateThemeColor(color: ColorType) {
        Logger.d(label, "Update theme color to $color")
        viewModelScope.launch {
            repository.updateThemeColor(color)
        }
    }

    fun updateDarkThemeStyle(style: DarkThemeStyle) {
        Logger.d(label, "Update dark theme style to $style")
        viewModelScope.launch {
            repository.updateThemeStyle(style)
        }
    }

    fun agreePolicy() {
        Logger.d(label, "Agree policy")
        viewModelScope.launch {
            repository.agreePolicy()
        }
    }

    suspend fun checkUpdate(): GitHubRelease? {
        val proxy = repository.getSettingsSnapshot().proxy
        val service = CheckUpdateClient.getService(proxy)
        val result = service.getLatestRelease()
        when (result) {
            is NetworkResult.Success -> {
                val input = result.response.name
                val regex = Regex("""Version Code:\s*(\d+)""")
                val matchResult = regex.find(input)
                val versionCode = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
                Logger.d(label, "Current version code: ${BuildConfig.VERSION_CODE}")
                Logger.d(label, "Latest version code: $versionCode")
                if (BuildConfig.VERSION_CODE < versionCode) {
                    Logger.d(label, "Update available: ${result.response.tagName}")
                    updateRelease.value = result.response
                    showUpdateDialog()
                    return result.response
                } else {
                    Logger.d(label, "No update available")
                    dismissUpdateDialog()
                    return null
                }
            }

            is NetworkResult.Failure -> {
                Logger.d(label, "Check update failed: $result")
                dismissUpdateDialog()
                return null
            }

            is NetworkResult.Error -> {
                Logger.d(label, "Check update failed: $result")
                dismissUpdateDialog()
                return null
            }
        }

    }

    fun dismissUpdateDialog() {
        updateDismissed.value = true
    }

    fun showUpdateDialog() {
        updateDismissed.value = false
    }
}

class MainViewModelFactory(
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}