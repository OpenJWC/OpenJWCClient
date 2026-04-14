package org.openjwc.client.viewmodels

import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.repository.ChatRepository
import org.openjwc.client.data.repository.ChatStreamStatus
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.FetchedNotice

sealed class ChatSessionState {
    data object Idle : ChatSessionState()
    data object Loading : ChatSessionState()      // 刚发出请求，等待响应
    data object Generating: ChatSessionState()     // 正在生成
    data object ToolCalling : ChatSessionState()  // AI 正在查课表或爬取网页
    data class Error(val msg: String) : ChatSessionState()
}

class ChatViewModel(
    private val settingsRepository: SettingsRepository,
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


    var uiEvent = Channel<UiEvent>(Channel.BUFFERED)
        private set

    var navEvent = Channel<NavEvent>(Channel.BUFFERED)
        private set

    var attachments = MutableStateFlow<List<FetchedNotice>>(emptyList())
        private set

    var inputText = MutableStateFlow("")
        private set

    fun updateInputText(newText: String) {
        inputText.value = newText
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = currentSessionMetadata
        .flatMapLatest { metadata ->
            if (metadata == null) flowOf(emptyList())
            else chatRepository.getMessagesBySessionId(metadata.sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions = chatRepository.getChatSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                    sessionId = chatRepository.createChatSession(messageText.take(20))
                    val newMetadata = ChatMetadata(sessionId = sessionId, title = messageText.take(20))
                    currentSessionMetadata.value = newMetadata
                }

                chatRepository.sendMessage(
                    sessionId,
                    messageText,
                    currentAttachments,
                    settingsRepository.getSettingsSnapshot()
                ).collect { status ->
                    // 1. 处理导航逻辑 (401 跳转)
                    if (status is ChatStreamStatus.Failure && status.code == 401) {
                        navEvent.send(NavEvent.ToLogin())
                    }

                    // 2. 映射 UI 状态
                    updateSessionState(sessionId, when(status) {
                        is ChatStreamStatus.Loading -> ChatSessionState.Loading
                        is ChatStreamStatus.ToolCalling -> ChatSessionState.ToolCalling
                        is ChatStreamStatus.Generating -> ChatSessionState.Generating
                        is ChatStreamStatus.Finished -> ChatSessionState.Idle
                        is ChatStreamStatus.Failure -> ChatSessionState.Error(status.msg)
                    })
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
            if (currentSessionMetadata.value?.sessionId == sessionId) {
                currentSessionMetadata.value = null
            }
        }
    }

    fun copyMessage(
        message: ChatMessage,
        clipboardManager: Clipboard,
        context: Context
    ) {
        viewModelScope.launch {
            Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show()
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