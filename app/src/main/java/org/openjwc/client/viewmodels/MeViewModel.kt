package org.openjwc.client.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.datastore.CachedHitokoto
import org.openjwc.client.data.repository.AuthRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.net.models.NetworkResult
import java.time.LocalDate

class MeViewModel(
    private val repository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val tag = "MeViewModel"

    var hitokoto = repository.hitokotoFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CachedHitokoto()
    )

    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set

    var navEvent =Channel<NavEvent>(Channel.BUFFERED)
        private set

    fun refreshHitokotoLazily() {
        viewModelScope.launch {
            if (hitokoto.value.date != LocalDate.now().toString()) {
                repository.tryRefreshHitokoto()
            }
        }
    }

    fun refreshHitokoto(successMessage: String) {
        viewModelScope.launch {
            val result = repository.tryRefreshHitokoto()
            when (result) {
                is NetworkResult.Failure -> {
                    uiEvent.send(UiEvent.ShowToast("(${result.code}) ${result.msg}"))
                    if (result.code == 401) {
                        authRepository.clearSession()
                        navEvent.send(NavEvent.ToLogin())
                    }
                }

                is NetworkResult.Error -> uiEvent.send(UiEvent.ShowToast(result.msg))
                is NetworkResult.Success -> uiEvent.send(UiEvent.ShowToast(successMessage)) // TODO: 也要迁移
            }
        }
    }
}


class MeViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeViewModel(settingsRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
