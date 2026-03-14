package org.openjwc.client.viewmodels

import android.util.Log
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
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.net.chat.ChatClient
import org.openjwc.client.net.chat.ChatMessage
import org.openjwc.client.net.chat.sendMessageStream
import org.openjwc.client.net.models.ChatHistory
import org.openjwc.client.net.models.ChatRequest
import org.openjwc.client.net.models.NetworkResult

sealed class SendMessageState {
    data object Idle : SendMessageState()
    data object Sending : SendMessageState()
}

sealed class ChatEvent {
    data class ShowToast(val message: String) : ChatEvent()
    data class ShowSnackBar(val message: String) : ChatEvent()
}

class ChatViewModel(
    private val repository: SettingsRepository
) : ViewModel() {
    private val label = "ChatViewModel"
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // 控制发送按钮是否可用
    private val _sendMessageState = MutableStateFlow<SendMessageState>(SendMessageState.Idle)
    val sendMessageState: StateFlow<SendMessageState> = _sendMessageState.asStateFlow()

    private val _eventChannel = Channel<ChatEvent>()
    val events = _eventChannel.receiveAsFlow()

    val settings: StateFlow<UserSettings> = repository.userSettings
        .map { it ?: UserSettings() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    fun sendMessage(message: String) {
        if (sendMessageState.value is SendMessageState.Sending
            || message.isBlank()) return

        Log.d(label, "Sending message: $message")
        _sendMessageState.value = SendMessageState.Sending

        val userMsgId = System.currentTimeMillis()
        val botMsgId = userMsgId + 1
        val newUserMessage = ChatMessage(id = userMsgId, text = message, isUser = true, isLoading = false)

        _messages.update { it + newUserMessage }

        viewModelScope.launch {
            val currentSettings = repository.getSettingsSnapshot() ?: UserSettings()
            val host = currentSettings.host
            val port = currentSettings.port

            val apiService = ChatClient.createService(host, port)
            Log.d("ChatViewModel", "API service created")
            try {
                // 准备历史记录
                val historyList = _messages.value
                    .filter { it.id != userMsgId }
                    .map {
                        ChatHistory(
                            role = if (it.isUser) "user" else "assistant",
                            content = it.text
                        )
                    }

                // TODO: noticeId 还不知道怎么用
                val chatRequest = ChatRequest("test", message, true, historyList)

                // 插入占位符
                _messages.update { it + ChatMessage(id = botMsgId, text = "", isUser = false, isLoading = true) }
                Log.d(label, "Stream collection started, connected to $host:$port")
                // 开始流式收集
                apiService.sendMessageStream(chatRequest).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            _messages.update { list ->
                                list.map { msg ->
                                    if (msg.id == botMsgId) msg.copy(text = result.content)
                                    else msg
                                }
                            }
                        }

                        is NetworkResult.ValidationError -> {
                            _messages.update { list -> list.filter { it.id != botMsgId } }
                            _eventChannel.send(ChatEvent.ShowToast(result.errors.detail.toString()))
                        }

                        is NetworkResult.Failure -> {
                            _messages.update { list -> list.filter { it.id != botMsgId } }
                            _eventChannel.send(ChatEvent.ShowToast("请求失败(${result.code}): ${result.msg}"))
                        }

                        is NetworkResult.Error -> {
                            _messages.update { list -> list.filter { it.id != botMsgId } }
                            _eventChannel.send(ChatEvent.ShowToast("请求失败: ${result.msg}"))
                        }
                    }
                }
                Log.d(label, "Stream collection finished successfully")
            } catch (e: Exception) {
                _messages.update { list -> list.filter { it.id != botMsgId } }
                Log.e(label, "Stream collection failed", e)
            } finally {
                _messages.update { list ->
                    list.map { msg ->
                        msg.copy(isLoading = false)
                    }
                }
                _sendMessageState.value = SendMessageState.Idle
                Log.d(label, "Stream collection finished finally")
            }
        }
    }
}

class ChatViewModelFactory(
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // 💡 传入两个 Repository
            return ChatViewModel(settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}