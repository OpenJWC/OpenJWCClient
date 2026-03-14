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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.repository.ChatRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.net.chat.ChatClient
import org.openjwc.client.net.chat.ChatMessage
import org.openjwc.client.net.chat.ChatMetadata
import org.openjwc.client.net.chat.Role
import org.openjwc.client.net.chat.sendMessageStream
import org.openjwc.client.net.models.ChatHistory
import org.openjwc.client.net.models.ChatRequestBody
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
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    // 1. 状态定义
    private val _currentSessionMetadata = MutableStateFlow<ChatMetadata?>(null)
    val currentSessionMetadata = _currentSessionMetadata.asStateFlow()

    private val _sendMessageState = MutableStateFlow<SendMessageState>(SendMessageState.Idle)
    val sendMessageState = _sendMessageState.asStateFlow()

    private val _eventChannel = Channel<ChatEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _currentSessionMetadata
        .flatMapLatest { metadata ->
            if (metadata == null) flowOf(emptyList())
            else chatRepository.getMessagesBySessionId(metadata.sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions = chatRepository.getChatSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. 业务逻辑
    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val metadata = chatRepository.getChatMetadataById(sessionId)
            _currentSessionMetadata.value = metadata.first()
        }
    }

    fun toNewChat() {
        _currentSessionMetadata.value = null
    }
    fun sendMessage(messageContent: String) {
        if (sendMessageState.value is SendMessageState.Sending || messageContent.isBlank()) return

        _sendMessageState.value = SendMessageState.Sending

        viewModelScope.launch {
            var aiMsgId: Long? = null
            try {
                var sessionId = _currentSessionMetadata.value?.sessionId
                if (sessionId == null) {
                    sessionId = chatRepository.createChatSession(messageContent.take(20))
                    // 立即更新 metadata 以便让 messages 流切换到新会话
                    _currentSessionMetadata.value = ChatMetadata(sessionId = sessionId, title = messageContent.take(20))
                }

                chatRepository.sendMessage(ChatMessage(ownerSessionId = sessionId, text = messageContent, role = Role.USER))

                val historyList = messages.value
                    .filter { it.text.isNotBlank() }
                    .map { ChatHistory(role = if (it.role == Role.USER) "user" else "assistant", content = it.text) }

                val currentSettings = settingsRepository.getSettingsSnapshot() ?: UserSettings()
                val apiService = ChatClient.createService(currentSettings.host, currentSettings.port)

                aiMsgId = chatRepository.sendMessage(ChatMessage(ownerSessionId = sessionId, text = "", role = Role.ASSISTANT))

                apiService.sendMessageStream(currentSettings.authKey, settingsRepository.getOrGenerateDeviceId(),
                    ChatRequestBody("test", messageContent, true, historyList)
                ).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            chatRepository.updateMessageText(aiMsgId, result.content)
                        }
                        is NetworkResult.Failure -> throw Exception("请求失败(${result.code}): ${result.msg}")
                        is NetworkResult.Error -> throw Exception(result.msg)
                        is NetworkResult.ValidationError -> throw Exception("验证失败: ${result.errors.detail}")
                    }
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "sendMessage Error", e)
                handleFailure(e.localizedMessage ?: "Unknown error", aiMsgId)
            } finally {
                Log.d("ChatViewModel", "Setting state to Idle")
                _sendMessageState.value = SendMessageState.Idle
            }
        }
    }

    private suspend fun handleFailure(errorMsg: String, aiMsgId: Long? = null) {
        aiMsgId?.let { chatRepository.deleteMessageById(it) }
        _eventChannel.send(ChatEvent.ShowToast(errorMsg))
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_currentSessionMetadata.value?.sessionId == sessionId) {
                _currentSessionMetadata.value = null
            }
        }
    }
}

class ChatViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(settingsRepository, chatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}