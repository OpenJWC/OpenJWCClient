package org.openjwc.client.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.openjwc.client.data.datastore.LlmKeyStore
import org.openjwc.client.data.datastore.LlmSettingsDataSource
import org.openjwc.client.log.Logger
import org.openjwc.client.net.llm.LlmClientFactory
import org.openjwc.client.net.llm.LlmDelta
import org.openjwc.client.net.llm.LlmMessage
import org.openjwc.client.net.llm.LlmPresets
import org.openjwc.client.net.llm.LlmProviderConfig

data class LlmSettingsUiState(
    val config: LlmProviderConfig = LlmProviderConfig(),
    val apiKey: String = "",
    val testing: Boolean = false,
    val testResult: String? = null,
    val testError: String? = null,
) {
    val hasKey: Boolean get() = apiKey.isNotBlank()
}

class LlmSettingsViewModel(
    private val settingsDataSource: LlmSettingsDataSource,
    private val keyStore: LlmKeyStore,
) : ViewModel() {

    private val label = "LlmSettingsVM"

    private val _uiState = MutableStateFlow(LlmSettingsUiState())
    val uiState: StateFlow<LlmSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val config = settingsDataSource.current()
            val key = keyStore.get(config.providerId).orEmpty()
            _uiState.update { it.copy(config = config, apiKey = key) }
        }
    }

    fun selectProvider(presetId: String) {
        val preset = LlmPresets.byId(presetId)
        val config = _uiState.value.config.copy(
            providerId = preset.id,
            protocol = preset.protocol,
            baseUrl = preset.baseUrl.ifBlank { _uiState.value.config.baseUrl },
            model = preset.defaultModel.ifBlank { _uiState.value.config.model },
        )
        val key = keyStore.get(config.providerId).orEmpty()
        _uiState.update {
            it.copy(config = config, apiKey = key, testResult = null, testError = null)
        }
        persist(config)
    }

    fun updateBaseUrl(value: String) = updateConfig { it.copy(baseUrl = value.trim()) }
    fun updateModel(value: String) = updateConfig { it.copy(model = value.trim()) }
    fun updateTemperature(value: Float) = updateConfig { it.copy(temperature = value) }

    private fun updateConfig(transform: (LlmProviderConfig) -> LlmProviderConfig) {
        val config = transform(_uiState.value.config)
        _uiState.update { it.copy(config = config, testResult = null, testError = null) }
        persist(config)
    }

    private fun persist(config: LlmProviderConfig) {
        viewModelScope.launch { settingsDataSource.save(config) }
    }

    fun updateApiKey(value: String) {
        val providerId = _uiState.value.config.providerId
        _uiState.update { it.copy(apiKey = value, testResult = null, testError = null) }
        viewModelScope.launch {
            if (value.isBlank()) keyStore.clear(providerId) else keyStore.save(providerId, value)
        }
    }

    /**
     * 测试连接。
     * 参数为 null 时用已保存的配置；界面传入输入框里的当前值即可「不保存直接测试」。
     */
    fun testConnection(
        baseUrl: String? = null,
        model: String? = null,
        apiKey: String? = null,
    ) {
        val current = _uiState.value
        val config = current.config.copy(
            baseUrl = (baseUrl ?: current.config.baseUrl).trim(),
            model = (model ?: current.config.model).trim(),
        )
        val key = (apiKey ?: current.apiKey).trim()
        when {
            key.isBlank() -> {
                _uiState.update { it.copy(testError = "请先填写 API Key", testResult = null) }
                return
            }

            config.baseUrl.isBlank() -> {
                _uiState.update { it.copy(testError = "请先填写 Base URL", testResult = null) }
                return
            }

            config.model.isBlank() -> {
                _uiState.update { it.copy(testError = "请先填写模型名称", testResult = null) }
                return
            }
        }
        Logger.i(label, "测试连接 provider=${config.providerId} model=${config.model}")
        _uiState.update { it.copy(testing = true, testError = null, testResult = null) }
        viewModelScope.launch {
            val client = LlmClientFactory.create(config, key)
            val outcome = withTimeoutOrNull(TEST_TIMEOUT_MS) {
                runCatching {
                    var text = ""
                    client.streamChat(
                        messages = listOf(LlmMessage.user("ping")),
                        tools = emptyList(),
                    ).collect { delta ->
                        when (delta) {
                            is LlmDelta.Text -> text += delta.value
                            is LlmDelta.Finished -> throw TestDone(text)
                            else -> Unit
                        }
                    }
                    text
                }
            }
            when {
                outcome == null -> _uiState.update {
                    it.copy(testing = false, testError = "连接超时")
                }

                outcome.isSuccess -> {
                    val reply = outcome.getOrNull().orEmpty().ifBlank { "连接成功" }
                    Logger.i(label, "测试连接成功：${reply.take(80)}")
                    _uiState.update {
                        it.copy(testing = false, testResult = reply.take(200))
                    }
                }

                outcome.exceptionOrNull() is TestDone -> {
                    val reply = (outcome.exceptionOrNull() as TestDone).text.ifBlank { "连接成功" }
                    Logger.i(label, "测试连接成功：${reply.take(80)}")
                    _uiState.update {
                        it.copy(testing = false, testResult = reply.take(200))
                    }
                }

                else -> {
                    val error = outcome.exceptionOrNull()
                    Logger.e(label, "test failed: ${error?.message}", error)
                    _uiState.update {
                        it.copy(testing = false, testError = error?.localizedMessage ?: "连接失败")
                    }
                }
            }
        }
    }

    private class TestDone(val text: String) : RuntimeException("done")

    private companion object {
        const val TEST_TIMEOUT_MS = 30_000L
    }
}

class LlmSettingsViewModelFactory(
    private val settingsDataSource: LlmSettingsDataSource,
    private val keyStore: LlmKeyStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LlmSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LlmSettingsViewModel(settingsDataSource, keyStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
