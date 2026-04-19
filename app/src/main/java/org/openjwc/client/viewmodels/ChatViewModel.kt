package org.openjwc.client.viewmodels

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.Role
import org.openjwc.client.data.repository.ChatRepository
import org.openjwc.client.data.repository.ChatStreamStatus
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.FetchedNotice

sealed class ChatSessionState {
    data object Idle : ChatSessionState()
    data object Loading : ChatSessionState()      // 刚发出请求，等待响应
    data object Generating : ChatSessionState()     // 正在生成
    data object ToolCalling : ChatSessionState()  // AI 正在查课表或爬取网页
    data class Error(val msg: String) : ChatSessionState()
}

data class ChatSessionUiModel(
    val metadata: ChatMetadata,
    val state: ChatSessionState
)

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val label = "ChatViewModel"
    private val _sessionStates = MutableStateFlow<Map<Long?, ChatSessionState>>(emptyMap())

    fun getSessionState(sessionId: Long?): StateFlow<ChatSessionState> {
        return _sessionStates.map { it[sessionId] ?: ChatSessionState.Idle }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatSessionState.Idle)
    }

    private fun updateSessionState(sessionId: Long?, state: ChatSessionState) {
        _sessionStates.value = _sessionStates.value + (sessionId to state)
    }

    var currentSessionMetadata = MutableStateFlow<ChatMetadata?>(null)
        private set

    // 用来记录正在生成的文本，key 是 sessionId，value 是生成的文本
    private val _generatingTexts = MutableStateFlow<Map<Long, String>>(emptyMap())


    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set

    var navEvent = Channel<NavEvent>(Channel.BUFFERED)
        private set

    var attachments = MutableStateFlow<List<FetchedNotice>>(emptyList())
        private set

    var inputText = MutableStateFlow("")
        private set

    fun updateInputText(newText: String) {
        inputText.value = newText.take(10000)
    }

    fun addAttachment(attachment: FetchedNotice) {
        if (attachment in attachments.value) return
        Logger.d(label, "addAttachment: $attachment")
        attachments.value = attachments.value + attachment
    }

    fun deleteAttachment(attachment: FetchedNotice) {
        attachments.value = attachments.value - attachment
    }

    fun clearAttachments() {
        attachments.value = emptyList()
    }

    /// Repository 以 500ms 的周期写数据库，同时 UI 立刻渲染出来
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = combine(
        currentSessionMetadata.flatMapLatest { metadata ->
            if (metadata == null) flowOf(emptyList())
            else chatRepository.getMessagesBySessionId(metadata.sessionId)
        },
        _generatingTexts
    ) { dbMessages, generatingMap ->
        val currentSessionId = currentSessionMetadata.value?.sessionId
        val liveText = generatingMap[currentSessionId]
        if (liveText != null) {
            dbMessages.mapIndexed { index, msg ->
                if (index == dbMessages.lastIndex && msg.role == Role.ASSISTANT) {
                    msg.copy(text = liveText)
                } else {
                    msg
                }
            }
        } else {
            dbMessages
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<ChatSessionUiModel>> = combine(
        chatRepository.getChatSessions(),
        _sessionStates
    ) { sessions, states ->
        sessions.map { session ->
            ChatSessionUiModel(
                metadata = session.metadata,
                state = states[session.metadata.sessionId] ?: ChatSessionState.Idle
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val metadata = chatRepository.getChatMetadataById(sessionId)
            currentSessionMetadata.value = metadata.first()
        }
    }

    fun toNewChat() {
        currentSessionMetadata.value = null
    }

    /*fun sendMessage() {
        if (sendMessageState.value is SendMessageState.Sending || inputText.value.isBlank()) return

        sendMessageState.value = SendMessageState.Sending
        val messageText = inputText.value
        val attachments = attachments.value
        viewModelScope.launch {
            clearAttachments()
            updateInputText("")
            try {
                var sessionId = currentSessionMetadata.value?.sessionId
                if (sessionId == null) {
                    sessionId = chatRepository.createChatSession(messageText.take(20))
                    currentSessionMetadata.value =
                        ChatMetadata(sessionId = sessionId, title = messageText.take(20))
                }

                val result = chatRepository.sendMessage(
                    sessionId,
                    messageText,
                    attachments,
                    settingsRepository.getSettingsSnapshot()
                )

                if(result is ChatNetworkResult.Failure && result.code == 401) {
                    navEvent.send(NavEvent.ToLogin())
                }
            } catch (e: Exception) {
                Logger.e(label, "sendMessage Error", e)
                uiEvent.send(UiEvent.ShowToast(e.localizedMessage ?: "Unknown Error"))
            } finally {
                Logger.d(label, "Setting state to Idle")
                sendMessageState.value = SendMessageState.Idle
            }
        }
    }*/
    fun sendMessage() {
        val messageText = inputText.value
        if (messageText.isBlank()) return
        updateInputText("")
        val currentAttachments = attachments.value
        clearAttachments()

        viewModelScope.launch {
            try {
                var sessionId = currentSessionMetadata.value?.sessionId
                if (sessionId == null) {
                    sessionId =
                        chatRepository.createChatSession(messageText.replace("\n", "").take(20))
                    val newMetadata = ChatMetadata(
                        sessionId = sessionId,
                        title = messageText.replace("\n", "").take(20)
                    )
                    currentSessionMetadata.value = newMetadata
                }

                chatRepository.sendMessage(sessionId, messageText, currentAttachments)
                    .collect { status ->
                        if (status is ChatStreamStatus.Generating) {
                            _generatingTexts.update { it + (sessionId to status.content) }
                        }

                        // 流结束或失败时，清理内存占位
                        if (status is ChatStreamStatus.Finished || status is ChatStreamStatus.Failure) {
                            _generatingTexts.update { it - sessionId }
                        }

                        // 更新会话状态 (Loading/Generating/Idle)
                        updateSessionState(
                            sessionId, when (status) {
                                is ChatStreamStatus.Loading -> ChatSessionState.Loading
                                is ChatStreamStatus.Generating -> ChatSessionState.Generating
                                is ChatStreamStatus.Finished -> ChatSessionState.Idle
                                is ChatStreamStatus.Failure -> {
                                    if (status.code == 401) {
                                        navEvent.send(NavEvent.ToLogin())
                                    }
                                    uiEvent.send(UiEvent.ShowToast(status.msg))
                                    ChatSessionState.Error(status.msg)
                                }

                                else -> ChatSessionState.Idle
                            }
                        )
                    }
            } catch (e: Exception) {
                Logger.e(label, "sendMessage Error", e)
                uiEvent.send(UiEvent.ShowToast(e.localizedMessage ?: "Unknown Error"))
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            _sessionStates.value -= sessionId
            if (currentSessionMetadata.value?.sessionId == sessionId) {
                currentSessionMetadata.value = null
            }
        }
    }

    fun copyMessage(
        message: ChatMessage,
        clipboardManager: Clipboard,
    ) {
        viewModelScope.launch {
            uiEvent.send(UiEvent.ShowToast("复制成功"))
            clipboardManager.setClipEntry(
                ClipEntry(
                    ClipData.newPlainText(
                        message.text,
                        message.text
                    )
                )
            )
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            chatRepository.deleteMessageById(messageId)
        }
    }

    fun updateMetadata(metadata: ChatMetadata) {
        viewModelScope.launch {
            chatRepository.updateMetadata(metadata)
            currentSessionMetadata.value = metadata
        }
    }
}

class ChatViewModelFactory(
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}