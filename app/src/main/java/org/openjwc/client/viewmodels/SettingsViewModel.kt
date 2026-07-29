package org.openjwc.client.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.datastore.UserSettings
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.DevicesQueryResponseData
import org.openjwc.client.net.models.DevicesUnbindSuccessResponse
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.Proxy
import org.openjwc.client.net.models.SuccessResponse
import org.openjwc.client.utils.changeAppLanguage

private const val label = "SettingsViewModel"

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set

    val settings: StateFlow<UserSettings> = settingsRepository.userSettings
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = UserSettings())

    fun updateHost(host: String) = viewModelScope.launch { settingsRepository.updateHost(host) }
    fun updatePort(port: Int) = viewModelScope.launch { settingsRepository.updatePort(port) }
    fun updateUseHttp(useHttp: Boolean) = viewModelScope.launch { settingsRepository.updateUseHttp(useHttp) }
    fun updateFreshDays(freshDays: Int) = viewModelScope.launch { settingsRepository.updateFreshDays(freshDays) }

    fun updateBackground(uri: Uri) = viewModelScope.launch {
        val success = settingsRepository.updateBackground(uri)
        if (!success) uiEvent.send(UiEvent.ShowToast(UiText.DynamicString("设置背景失败")))
    }

    fun deleteBackground() = viewModelScope.launch { settingsRepository.deleteBackground() }
    fun updateBackgroundAlpha(alpha: Float) = viewModelScope.launch { settingsRepository.updateBackgroundAlpha(alpha) }
    fun updateProxy(proxy: Proxy) = viewModelScope.launch { settingsRepository.updateProxy(proxy) }

    fun updateLanguage(code: String?) = viewModelScope.launch {
        changeAppLanguage(code)
        settingsRepository.updateLanguageCode(code)
    }

    fun updateShowTimeline(show: Boolean) = viewModelScope.launch { settingsRepository.updateShowTimeline(show) }
    fun updateShowDate(show: Boolean) = viewModelScope.launch { settingsRepository.updateShowDate(show) }
    fun updateShowPeriodTime(show: Boolean) = viewModelScope.launch { settingsRepository.updateShowPeriodTime(show) }
    fun updateShowNonCurrentWeek(show: Boolean) = viewModelScope.launch { settingsRepository.updateShowNonCurrentWeek(show) }

    private var _deviceResult = MutableStateFlow<NetworkResult<SuccessResponse<DevicesQueryResponseData>>>(
        NetworkResult.Success(SuccessResponse("success", DevicesQueryResponseData(deviceQueries = emptyList())))
    )
    private var _isLoadingDeviceResult = MutableStateFlow(false)
    val isLoadingDeviceResult = _isLoadingDeviceResult.asStateFlow()
    val deviceResult = _deviceResult.asStateFlow()

    private var _deviceUnbindNetworkResult = MutableStateFlow<NetworkResult<DevicesUnbindSuccessResponse>>(
        NetworkResult.Success(DevicesUnbindSuccessResponse(""))
    )
    val deviceUnbindNetworkResult = _deviceUnbindNetworkResult.asStateFlow()

    fun devicesQuery() {
        viewModelScope.launch {
            _isLoadingDeviceResult.value = true
            _deviceResult.value = authRepository.deviceQuery()
            _isLoadingDeviceResult.value = false
        }
    }

    fun unbindAndRefresh(deviceId: String) {
        viewModelScope.launch {
            _isLoadingDeviceResult.value = true
            val unbindResult = authRepository.deviceUnbind(deviceId)
            _deviceUnbindNetworkResult.value = unbindResult
            if (unbindResult is NetworkResult.Success) {
                devicesQuery()
                _isLoadingDeviceResult.value = false
            }
        }
    }

    fun clearUnbindResult() {
        _deviceUnbindNetworkResult.value = NetworkResult.Success(DevicesUnbindSuccessResponse(""))
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
