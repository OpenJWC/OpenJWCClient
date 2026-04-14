package org.openjwc.client.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.BuildConfig
import org.openjwc.client.data.datastore.UserSettings
import org.openjwc.client.data.repository.SettingsRepository
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
    private val label = "MainViewModel"
    private val _currentTab = MutableStateFlow<MainTab>(MainTab.Chat)

    val currentTab = _currentTab.asStateFlow()

    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set

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


    private val _updateEvent = Channel<GitHubRelease>()
    val updateEvent = _updateEvent.receiveAsFlow()

    var updateRelease = MutableStateFlow<GitHubRelease?>(null)
        private set

    var showUpdateDialog = MutableStateFlow(false)
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

    fun checkUpdate(showToast: Boolean = true) {
        viewModelScope.launch {
            val proxy = repository.getSettingsSnapshot().proxy
            val service = CheckUpdateClient.getService(proxy)

            when (val result = service.getLatestRelease()) {
                is NetworkResult.Success -> {
                    val input = result.response.name
                    val regex = Regex("""Version Code:\s*(\d+)""")
                    val versionCode = regex.find(input)?.groupValues?.get(1)?.toIntOrNull() ?: 0

                    if (BuildConfig.VERSION_CODE < versionCode) {
                        Logger.d(label, "发现新版本: ${result.response.tagName}")
                        updateRelease.value = result.response
                        _updateEvent.send(result.response)
                    } else {
                        if(showToast) uiEvent.send(UiEvent.ShowToast("当前版本已是最新"))
                    }
                }
                else -> {
                    Logger.d(label, "检查更新失败或无更新")
                    uiEvent.send(UiEvent.ShowToast("检查更新失败或无更新"))
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        showUpdateDialog.value = false
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