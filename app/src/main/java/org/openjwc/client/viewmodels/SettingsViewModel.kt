package org.openjwc.client.viewmodels

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.VpnKey
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openjwc.client.data.datastore.UserSettings
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.Menu
import org.openjwc.client.data.settings.MenuItem
import org.openjwc.client.data.settings.SettingSection
import org.openjwc.client.data.settings.ToggleID
import org.openjwc.client.log.Logger
import org.openjwc.client.navigation.Screen
import org.openjwc.client.net.models.DevicesQueryResponseData
import org.openjwc.client.net.models.DevicesUnbindSuccessResponse
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.Proxy
import org.openjwc.client.net.models.SuccessResponse

private const val label = "SettingsViewModel"

sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
    data class ShowSnackBar(val message: String) : SettingsEvent()
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val menuTemplates = listOf(
        Menu(
            route = Screen.Settings, title = "设置", sections = listOf(
                SettingSection(
                    title = "通用", items = listOf(
                        MenuItem.Route(
                            icon = Icons.Default.Palette,
                            route = Screen.Theme,
                            title = "主题",
                        )
                    )
                ), SettingSection(
                    title = "连接", items = listOf(
                        MenuItem.Route(
                            icon = Icons.Default.Dns,
                            route = Screen.Host,
                            title = "网络设置",
                        ),
                        MenuItem.Route(
                            icon = Icons.Default.VpnKey,
                            route = Screen.Account,
                            title = "账户设置",
                        )
                    )
                ), SettingSection(
                    title = "资讯", items = listOf(
                        MenuItem.Route(
                            icon = Icons.Default.CalendarMonth,
                            route = Screen.NewsSettings,
                            title = "显示设置",
                        )
                    )
                ), SettingSection(
                    title = "调试", items = listOf(
                        MenuItem.Route(
                            icon = Icons.Default.BugReport,
                            route = Screen.Log,
                            title = "日志",
                        )
                    )
                )
            )
        )
    )

    val settings: StateFlow<UserSettings> = settingsRepository.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    private val _eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    fun updateHost(host: String) = viewModelScope.launch { settingsRepository.updateHost(host) }
    fun updatePort(port: Int) = viewModelScope.launch { settingsRepository.updatePort(port) }
    fun updateUseHttp(useHttp: Boolean) =
        viewModelScope.launch { settingsRepository.updateUseHttp(useHttp) }

    fun updateFreshDays(freshDays: Int) =
        viewModelScope.launch { settingsRepository.updateFreshDays(freshDays) }

    fun updateBackground(uri: Uri) =
        viewModelScope.launch { settingsRepository.updateBackground(uri) }

    fun deleteBackground() = viewModelScope.launch { settingsRepository.deleteBackground() }
    fun updateBackgroundAlpha(alpha: Float) =
        viewModelScope.launch { settingsRepository.updateBackgroundAlpha(alpha) }

    fun updateProxy(proxy: Proxy) = viewModelScope.launch { settingsRepository.updateProxy(proxy) }

    private var _deviceResult =
        MutableStateFlow<NetworkResult<SuccessResponse<DevicesQueryResponseData>>>(
            NetworkResult.Success(
                response = SuccessResponse(
                    message = "success",
                    data = DevicesQueryResponseData(
//                        limitedDeviceCount = 3,
                        deviceQueries = emptyList()
                    )
                )
            )
        )
    private var _isLoadingDeviceResult = MutableStateFlow(false)
    val isLoadingDeviceResult = _isLoadingDeviceResult.asStateFlow()
    val deviceResult = _deviceResult.asStateFlow()

    private var _deviceUnbindNetworkResult =
        MutableStateFlow<NetworkResult<DevicesUnbindSuccessResponse>>(
            NetworkResult.Success(DevicesUnbindSuccessResponse(""))
        )

    val deviceUnbindNetworkResult = _deviceUnbindNetworkResult.asStateFlow()

    fun devicesQuery() {
        viewModelScope.launch {
            Logger.d(label, "devicesQuery start...")
            _isLoadingDeviceResult.value = true
            val result = authRepository.deviceQuery()
            if (result is NetworkResult.Failure && result.code == 401) authRepository.clearSession()
            _deviceResult.value = result
            Logger.d(label, "devicesQuery end...")
            _isLoadingDeviceResult.value = false
        }
    }

    fun unbindAndRefresh(deviceId: String) {
        viewModelScope.launch {
            _isLoadingDeviceResult.value = true
            Logger.d(label, "执行解绑...")
            val unbindResult = authRepository.deviceUnbind(deviceId)
            Logger.d(label, "Unbind result: $unbindResult")
            _deviceUnbindNetworkResult.value = unbindResult
            if (unbindResult is NetworkResult.Success) {
                Logger.d(label, "解绑成功，开始刷新列表...")
                val result = authRepository.deviceQuery()
                _deviceResult.value = result
                _isLoadingDeviceResult.value = false
            } else if (unbindResult is NetworkResult.Failure && unbindResult.code == 401) {
                authRepository.clearSession()
            }
        }
    }

    fun clearUnbindResult() {
        _deviceUnbindNetworkResult.value = NetworkResult.Success(
            response = DevicesUnbindSuccessResponse("")
        )
    }

    // TODO: 设置里的每一个 Toggle 都得让 ViewModel 来保存状态。
    private val _toggleState = MutableStateFlow(
        mapOf(
            ToggleID.TEST_TOGGLE to false
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

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}