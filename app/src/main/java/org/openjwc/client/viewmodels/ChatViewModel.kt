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
import org.openjwc.client.net.chat.ChatSession
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

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _sendMessageState = MutableStateFlow<SendMessageState>(SendMessageState.Idle)
    val sendMessageState: StateFlow<SendMessageState> = _sendMessageState.asStateFlow()

    private val _eventChannel = Channel<ChatEvent>()
    val events = _eventChannel.receiveAsFlow()

    // 💡 唯一的数据源：自动响应 ID 变化切换监听
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else chatRepository.getMessagesBySessionId(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allSessions: StateFlow<List<ChatSession>> = chatRepository.getChatSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadSession(sessionId: Long) {
        _currentSessionId.value = sessionId
    }

    fun sendMessage(messageContent: String) {
        if (sendMessageState.value is SendMessageState.Sending || messageContent.isBlank()) return

        _sendMessageState.value = SendMessageState.Sending

        viewModelScope.launch {
            var aiMsgId: Long? = null // 提升作用域以便在 catch 中访问
            try {
                val sessionId = _currentSessionId.value ?: chatRepository.createChatSession(messageContent.take(20))
                if (_currentSessionId.value == null) _currentSessionId.value = sessionId

                // 1. 发送用户消息
                chatRepository.sendMessage(ChatMessage(ownerSessionId = sessionId, text = messageContent, role = Role.USER))

                // 2. 准备上下文 (使用 messages.value 代替 _messages.value)
                val historyList = messages.value
                    .filter { it.text.isNotBlank() }
                    .map { ChatHistory(role = if (it.role == Role.USER) "user" else "assistant", content = it.text) }

                // 3. 准备网络服务
                val currentSettings = settingsRepository.getSettingsSnapshot() ?: UserSettings()
                val apiService = ChatClient.createService(currentSettings.host, currentSettings.port)
                val chatRequestBody = ChatRequestBody("test", messageContent, true, historyList)

                // 4. 插入 AI 占位消息
                aiMsgId = chatRepository.sendMessage(ChatMessage(ownerSessionId = sessionId, text = "", role = Role.ASSISTANT))

                // 5. 流式更新
                apiService.sendMessageStream(currentSettings.authKey, settingsRepository.getOrGenerateDeviceId(), chatRequestBody)
                    .collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                chatRepository.updateMessageText(aiMsgId, result.content)
                            }
                            is NetworkResult.ValidationError -> handleFailure("验证失败: ${result.errors.detail}", aiMsgId)
                            is NetworkResult.Failure -> handleFailure("请求失败(${result.code}): ${result.msg}", aiMsgId)
                            is NetworkResult.Error -> handleFailure(result.msg, aiMsgId)
                        }
                    }

            } catch (e: Exception) {
                handleFailure("连接异常: ${e.localizedMessage}", aiMsgId)
            } finally {
                Log.d("ChatViewModel", "Setting state to Idle")
                _sendMessageState.value = SendMessageState.Idle
            }
        }
    }

    private suspend fun handleFailure(errorMsg: String, aiMsgId: Long? = null) {
        // 💡 如果报错了，可以考虑删除占位符，避免留下空白气泡
        Log.d("ChatViewModel", "handleFailure: $errorMsg")
        aiMsgId?.let { chatRepository.deleteMessageById(it) }
        _eventChannel.send(ChatEvent.ShowToast(errorMsg))
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