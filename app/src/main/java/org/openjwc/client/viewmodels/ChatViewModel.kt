package org.openjwc.client.viewmodels

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
import org.openjwc.client.R
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.ChatTurn
import org.openjwc.client.data.models.Role
import org.openjwc.client.agent.AgentFailure
import org.openjwc.client.data.repository.ChatRepository
import org.openjwc.client.data.repository.ChatStreamStatus
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.FetchedNotice

sealed class ChatSessionState {
    data object Idle : ChatSessionState()
    data object Loading : ChatSessionState()      // 刚发出请求，等待响应
    data object Generating : ChatSessionState()     // 正在生成
    data object ToolCalling : ChatSessionState()  // AI 正在检索本地资讯
    data class Error(val msg: String) : ChatSessionState()
}

data class ChatSessionUiModel(
    val metadata: ChatMetadata,
    val state: ChatSessionState
)

/** 失败的一轮，用于界面提示与重试 */
data class FailedTurn(
    val text: String,
    val attachments: List<FetchedNotice>,
    val message: String,
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val label = "ChatViewModel"
    private val _sessionStates = MutableStateFlow<Map<Long?, ChatSessionState>>(emptyMap())
    private val sessionStateCache = java.util.concurrent.ConcurrentHashMap<Long, StateFlow<ChatSessionState>>()
    private val NULL_SESSION_KEY = Long.MIN_VALUE

    fun getSessionState(sessionId: Long?): StateFlow<ChatSessionState> {
        val cacheKey = sessionId ?: NULL_SESSION_KEY
        return sessionStateCache.computeIfAbsent(cacheKey) {
            _sessionStates.map { it[sessionId] ?: ChatSessionState.Idle }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatSessionState.Idle)
        }
    }

    private fun updateSessionState(sessionId: Long?, state: ChatSessionState) {
        _sessionStates.update { it + (sessionId to state) }
    }

    var currentSessionMetadata = MutableStateFlow<ChatMetadata?>(null)
        private set

    // 用来记录正在生成的文本，key 是 sessionId，value 是生成的文本
    private val _generatingTexts = MutableStateFlow<Map<Long, String>>(emptyMap())

    // 失败的一轮，key 是 sessionId
    private val _failedTurns = MutableStateFlow<Map<Long, FailedTurn>>(emptyMap())
    private val failedTurnCache = java.util.concurrent.ConcurrentHashMap<Long, StateFlow<FailedTurn?>>()

    fun getFailedTurn(sessionId: Long?): StateFlow<FailedTurn?> {
        val cacheKey = sessionId ?: NULL_SESSION_KEY
        return failedTurnCache.computeIfAbsent(cacheKey) {
            _failedTurns.map { it[sessionId] }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        }
    }

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

    /**
     * 消息 + 工具轨迹。工具卡片由数据库驱动，重启后仍能还原；
     * 生成中的正文只存在于内存，避免流式期间频繁写库。
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val turns: StateFlow<List<ChatTurn>> = combine(
        currentSessionMetadata.flatMapLatest { metadata ->
            if (metadata == null) flowOf(emptyList())
            else chatRepository.observeTurns(metadata.sessionId)
        },
        _generatingTexts
    ) { dbTurns, generatingMap ->
        val currentSessionId = currentSessionMetadata.value?.sessionId
        val liveText = currentSessionId?.let { generatingMap[it] }
        if (liveText != null) {
            dbTurns.mapIndexed { index, turn ->
                if (index == dbTurns.lastIndex && turn.message.role == Role.ASSISTANT) {
                    turn.copy(message = turn.message.copy(text = liveText))
                } else {
                    turn
                }
            }
        } else {
            dbTurns
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
                runSend(sessionId, messageText, currentAttachments, isRetry = false)
            } catch (e: Exception) {
                Logger.e(label, "sendMessage Error", e)
                uiEvent.send(
                    UiEvent.ShowToast(
                        UiText.DynamicString(
                            e.localizedMessage ?: "Unknown Error"
                        )
                    )
                )
            }
        }
    }

    /** 失败后重试上一轮：复用已有的用户消息，不重复插入 */
    fun retryLastMessage() {
        val sessionId = currentSessionMetadata.value?.sessionId ?: return
        val failed = _failedTurns.value[sessionId] ?: return
        viewModelScope.launch {
            try {
                runSend(sessionId, failed.text, failed.attachments, isRetry = true)
            } catch (e: Exception) {
                Logger.e(label, "retry Error", e)
            }
        }
    }

    private suspend fun runSend(
        sessionId: Long,
        messageText: String,
        currentAttachments: List<FetchedNotice>,
        isRetry: Boolean,
    ) {
        // 新一轮问答：清空上一轮的失败态与内存正文
        _failedTurns.update { it - sessionId }
        _generatingTexts.update { it - sessionId }
        Logger.d(
            label,
            "sendMessage session=$sessionId retry=$isRetry textLen=${messageText.length} attachments=${currentAttachments.size}"
        )

        chatRepository.sendMessage(sessionId, messageText, currentAttachments, isRetry)
            .collect { status ->
                when (status) {
                    is ChatStreamStatus.Loading -> {
                        updateSessionState(sessionId, ChatSessionState.Loading)
                    }

                    is ChatStreamStatus.ToolRunning -> {
                        updateSessionState(sessionId, ChatSessionState.ToolCalling)
                    }

                    is ChatStreamStatus.Generating -> {
                        _generatingTexts.update { it + (sessionId to status.content) }
                        updateSessionState(sessionId, ChatSessionState.Generating)
                    }

                    is ChatStreamStatus.Finished -> {
                        Logger.d(label, "finished session=$sessionId answerLen=${status.finalContent.length}")
                        // 正文此时已落库；保留内存正文直到下一次发送，避免清理与 DB 反射之间的闪烁
                        _failedTurns.update { it - sessionId }
                        updateSessionState(sessionId, ChatSessionState.Idle)
                    }

                    is ChatStreamStatus.Failure -> {
                        Logger.e(label, "failure session=$sessionId code=${status.code} msg=${status.msg}")
                        _generatingTexts.update { it - sessionId }
                        _failedTurns.update {
                            it + (sessionId to FailedTurn(messageText, currentAttachments, status.msg))
                        }
                        if (status.errorCode in AgentFailure.CONFIG_RELATED) {
                            navEvent.send(NavEvent.ToLlmSettings())
                        }
                        uiEvent.send(UiEvent.ShowToast(UiText.DynamicString(status.msg)))
                        updateSessionState(sessionId, ChatSessionState.Error(status.msg))
                    }
                }
            }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            _sessionStates.update { it - sessionId }
            if (currentSessionMetadata.value?.sessionId == sessionId) {
                currentSessionMetadata.value = null
            }
        }
    }

    fun copyMessage(message: ChatMessage) {
        viewModelScope.launch {
            uiEvent.send(UiEvent.ShowToast(UiText.StringResource(R.string.copy_success)))
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
    private val chatRepository: ChatRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
