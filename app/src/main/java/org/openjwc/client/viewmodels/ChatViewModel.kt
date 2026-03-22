package org.openjwc.client.viewmodels

import android.content.ClipData
import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.Role
import org.openjwc.client.data.repository.ChatRepository
import org.openjwc.client.data.repository.SettingsRepository
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.net.chat.sendMessageStream
import org.openjwc.client.net.models.ChatHistory
import org.openjwc.client.net.models.ChatNetworkResult
import org.openjwc.client.net.models.ChatRequestBody
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.net.models.NetClient

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

    var currentSessionMetadata = MutableStateFlow<ChatMetadata?>(null)
        private set

    var sendMessageState = MutableStateFlow<SendMessageState>(SendMessageState.Idle)
        private set

    var eventChannel = Channel<ChatEvent>(Channel.BUFFERED)
        private set

    var attachments = MutableStateFlow<List<FetchedNotice>>(emptyList())
        private set

    var inputText = MutableStateFlow("")
        private set

    fun updateInputText(newText: String) {
        inputText.value = newText
    }
    fun addAttachment(attachment: FetchedNotice) {
        if(attachment in attachments.value) return
        Log.d("ChatViewModel", "addAttachment: $attachment")
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
    fun sendMessage() {
        if (sendMessageState.value is SendMessageState.Sending || inputText.value.isBlank()) return

        sendMessageState.value = SendMessageState.Sending
        val attachmentIds = attachments.value.map { it.id }
        val attachmentTitles = attachments.value.map { it.title}
        val messageText = inputText.value
        viewModelScope.launch {
            var aiMsgId: Long? = null
            clearAttachments()
            updateInputText("")
            try {
                var sessionId = currentSessionMetadata.value?.sessionId
                if (sessionId == null) {
                    sessionId = chatRepository.createChatSession(messageText.take(20))
                    // 立即更新 metadata 以便让 messages 流切换到新会话
                    currentSessionMetadata.value = ChatMetadata(sessionId = sessionId, title = messageText.take(20))
                }

                chatRepository.insertMessage(ChatMessage(ownerSessionId = sessionId, text = messageText, role = Role.USER, attachmentTitles = attachmentTitles))

                val historyList = messages.value
                    .filter { it.text.isNotBlank() }
                    .map { ChatHistory(role = if (it.role == Role.USER) "user" else "assistant", content = it.text) }

                val currentSettings = settingsRepository.getSettingsSnapshot() ?: UserSettings()
                val apiService = NetClient.getService(currentSettings.host, currentSettings.port, currentSettings.useHttp)

                aiMsgId = chatRepository.insertMessage(ChatMessage(ownerSessionId = sessionId, text = "", role = Role.ASSISTANT))

                apiService.sendMessageStream(currentSettings.authKey, settingsRepository.getOrGenerateDeviceId(),
                    ChatRequestBody(attachmentIds, messageText, true, historyList)
                ).collect { result ->
                    when (result) {
                        is ChatNetworkResult.Success -> {
                            chatRepository.updateMessageText(aiMsgId, result.content)
                        }
                        is ChatNetworkResult.Failure -> throw Exception("请求失败(${result.code}): ${result.msg}")
                        is ChatNetworkResult.Error -> throw Exception(result.msg)
                        is ChatNetworkResult.ValidationError -> throw Exception("验证失败: ${result.errors.detail}")
                    }
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "sendMessage Error", e)
                handleFailure(e.localizedMessage ?: "Unknown error", aiMsgId)
            } finally {
                Log.d("ChatViewModel", "Setting state to Idle")
                sendMessageState.value = SendMessageState.Idle
            }
        }
    }

    private suspend fun handleFailure(errorMsg: String, aiMsgId: Long? = null) {
        aiMsgId?.let { chatRepository.deleteMessageById(it) }
        eventChannel.send(ChatEvent.ShowToast(errorMsg))
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
            clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText(message.text, message.text)))
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