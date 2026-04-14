package org.openjwc.client.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.datastore.AuthSession
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.net.models.LoginSuccessResponse
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.SuccessResponse

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val tag = "AuthViewModel"

    val authSession = authRepository.authSession
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthSession()
        )
    var isLoggingIn = MutableStateFlow(false)
        private set

    var loginResult = MutableStateFlow<NetworkResult<SuccessResponse<LoginSuccessResponse>>?>(null)
        private set

    var registerResult =
        MutableStateFlow<NetworkResult<SuccessResponse<Map<String, String>>>?>(null)
        private set

    var isRegistering = MutableStateFlow(false)
        private set

    var navEvent = Channel<NavEvent>(Channel.BUFFERED)
        private set

    fun login(account: String, password: String) {
        viewModelScope.launch {
            isLoggingIn.value = true
            loginResult.value = authRepository.login(account, password)
            isLoggingIn.value = false
            if (loginResult.value is NetworkResult.Success) {
                navEvent.send(NavEvent.ToBack())
            }
        }
    }

    fun register(username: String, password: String, email: String) {
        viewModelScope.launch {
            isRegistering.value = true
            registerResult.value = authRepository.register(username, password, email)
            isRegistering.value = false
            if (registerResult.value is NetworkResult.Success) {
                navEvent.send(NavEvent.ToBack())
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }


    fun getOrCreateUuid() {
        viewModelScope.launch {
            authRepository.getOrCreateUuid()
        }
    }

    fun getOrCreateDeviceName() {
        viewModelScope.launch {
            authRepository.getOrCreateDeviceName()
        }
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}