package org.openjwc.client.viewmodels

import android.util.Log
import androidx.compose.material.icons.Icons
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
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.Menu
import org.openjwc.client.data.settings.MenuItem
import org.openjwc.client.data.settings.SettingSection
import org.openjwc.client.data.settings.ToggleID
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.net.auth.deviceUnbind
import org.openjwc.client.net.auth.devicesQuery
import org.openjwc.client.net.models.DeviceUnbindNetworkResult
import org.openjwc.client.net.models.DevicesQueryNetworkResult
import org.openjwc.client.net.models.NetClient
private const val label = "SettingsViewModel"

data class UiState(
    val showXXXDialog: Boolean = false
    // TODO: 如果有弹窗，就写在这里
)


sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
    data class ShowSnackBar(val message: String) : SettingsEvent()
}

class SettingsViewModel(
    private val repository: SettingsRepository
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
                            title = "服务器配置",
                        ),
                        MenuItem.Route(
                            icon = Icons.Default.VpnKey,
                            route = Screen.Auth,
                            title = "鉴权设置",
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
                )
            )
        )
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    // 这里的 uiState 交给 SettingsScreen 去监听，拿个 LaunchedEffect 去弹框吧

    val settings: StateFlow<UserSettings> = repository.userSettings
        .map { it ?: UserSettings() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    private val _eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()


    // 更新函数直接调用 repository
    fun updateAuthKey(key: String) = viewModelScope.launch { repository.updateAuthKey(key) }
    fun updateHost(host: String) = viewModelScope.launch { repository.updateHost(host) }
    fun updatePort(port: Int) = viewModelScope.launch { repository.updatePort(port) }
    fun updateUseHttp(useHttp: Boolean) = viewModelScope.launch { repository.updateUseHttp(useHttp) }
    fun updateFreshDays(freshDays: Int) = viewModelScope.launch { repository.updateFreshDays(freshDays) }
    private var _deviceResult = MutableStateFlow<DevicesQueryNetworkResult>(
        DevicesQueryNetworkResult.Success(
            response = org.openjwc.client.net.models.DevicesQuerySuccessResponse(
                message = "success",
                data = org.openjwc.client.net.models.DevicesQueryResponseData(
                    limitedDeviceCount = 3,
                    deviceIDs = emptyList()
                )
            )
        )
    )
    private var _isLoadingDeviceResult = MutableStateFlow(false)
    val isLoadingDeviceResult = _isLoadingDeviceResult.asStateFlow()
    val deviceResult = _deviceResult.asStateFlow()

    private var _deviceUnbindNetworkResult = MutableStateFlow<DeviceUnbindNetworkResult>(
        DeviceUnbindNetworkResult.Success(
            response = org.openjwc.client.net.models.DeviceUnbindSuccessResponse(
                detail = ""
            )
        )
    )

    val deviceUnbindNetworkResult = _deviceUnbindNetworkResult.asStateFlow()

    fun devicesQuery(){
        viewModelScope.launch {
            _isLoadingDeviceResult.value = true
            Log.d(label, "devicesQuery: start")
            try {
                val currentSettings = repository.getSettingsSnapshot() ?: UserSettings()
                Log.d(label, "devicesQuery: $currentSettings")
                val apiService = NetClient.getService(currentSettings.host, currentSettings.port, currentSettings.useHttp)
                val result = apiService.devicesQuery(
                    currentSettings.authKey,
                    currentSettings.uuidString
                )
                Log.d(label, "devicesQuery: $result")
                _deviceResult.value = result
            } catch (e: Exception) {
                handleFailure(e.localizedMessage ?: "Unknown error")
            } finally {
                _isLoadingDeviceResult.value = false
            }
        }
    }
    fun unbindAndRefresh(deviceId: String) {
        viewModelScope.launch {
            _isLoadingDeviceResult.value = true
            try {
                val currentSettings = repository.getSettingsSnapshot() ?: UserSettings()
                val apiService = NetClient.getService(currentSettings.host, currentSettings.port, currentSettings.useHttp)

                Log.d(label, "执行解绑...")
                val unbindResult =
                apiService.deviceUnbind(
                    currentSettings.authKey,
                    deviceId,
                )
                Log.d(label, "Unbind result: $unbindResult")
                _deviceUnbindNetworkResult.value = unbindResult

                if (unbindResult is DeviceUnbindNetworkResult.Success) {
                    Log.d(label, "解绑成功，开始刷新列表...")
                    val result = apiService.devicesQuery(
                        currentSettings.authKey,
                        currentSettings.uuidString
                    )
                    _deviceResult.value = result
                }
            } catch (e: Exception) {
                handleFailure(e.localizedMessage ?: "操作失败")
            } finally {
                _isLoadingDeviceResult.value = false
            }
        }
    }

    fun clearUnbindResult() {
        _deviceUnbindNetworkResult.value = DeviceUnbindNetworkResult.Success(
            response = org.openjwc.client.net.models.DeviceUnbindSuccessResponse(
                detail = ""
            )
        )
    }

    private suspend fun handleFailure(errorMsg: String) {
        Log.d(label, "handleFailure: $errorMsg")
        _eventChannel.send(SettingsEvent.ShowToast(errorMsg))
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
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}