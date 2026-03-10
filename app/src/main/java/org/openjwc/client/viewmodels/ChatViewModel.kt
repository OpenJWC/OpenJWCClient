package org.openjwc.client.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.openjwc.client.net.chat.ChatMessage
import org.openjwc.client.net.chat.sendMessageStream
import org.openjwc.client.net.models.ChatHistory
import org.openjwc.client.net.models.ChatRequest
import org.openjwc.client.net.models.NetworkResult

class ChatViewModel : ViewModel() {
    private val label = "ChatViewModel"
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        // 生成唯一的 ID，避免 System.currentTimeMillis() 在极快点击下重复
        val userMsgId = System.currentTimeMillis()
        val botMsgId = userMsgId + 1

        val newUserMessage = ChatMessage(id = userMsgId, text = message, isUser = true)

        // 先把用户消息塞进列表
        _messages.update { it + newUserMessage }

        viewModelScope.launch {
            // 准备历史记录, 并排除掉刚刚发出的这一条
            val historyList = _messages.value
                .filter { it.id != userMsgId }
                .map {
                    ChatHistory(
                        role = if (it.isUser) "user" else "assistant",
                        content = it.text
                    )
                }

            val chatRequest = ChatRequest(
                noticeId = "test",
                userQuery = message,
                stream = true,
                history = historyList
            )

            // 4. 插入一个 AI 的空占位符
            _messages.update { it + ChatMessage(id = botMsgId, text = "", isUser = false) }

            // 5. 开始收集流式数据
            try {
                sendMessageStream(chatRequest).collect { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            _messages.update { list ->
                                list.map { msg ->
                                    if (msg.id == botMsgId) msg.copy(text = result.content)
                                    else msg
                                }
                            }
                        }
                        is NetworkResult.ValidationError,
                        is NetworkResult.Failure,
                        is NetworkResult.Error -> {
                            _messages.update { list -> list.filter { it.id != botMsgId } }

                            Log.e(label, "Request failed: $result")
                        }
                    }
                }
            } catch (e: Exception) {
                _messages.update { list -> list.filter { it.id != botMsgId } }
                Log.e(label, "Stream collection failed", e)
            }
        }
    }
}