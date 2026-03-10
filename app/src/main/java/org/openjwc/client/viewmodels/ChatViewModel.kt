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
import org.openjwc.client.net.models.ChatHistory
import org.openjwc.client.net.models.ChatRequest
import org.openjwc.client.net.models.NetworkResult

class ChatViewModel : ViewModel() {
    private val label = "ChatViewModel"
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun sendMessage(message: String) {
        if (message.isBlank()) {
            Log.e(label, "Message is blank")
            return
        }

        val newMessage = ChatMessage(
            id = System.currentTimeMillis(), // TODO: ID 可能不是这个格式，先占个位
            text = message,
            isUser = true
        )

        // 网络和本地可能同时修改这个 messages, 所以用 update 确保原子性
        _messages.update { currentList ->
            currentList + newMessage
        }

        viewModelScope.launch {
            val chatRequest = ChatRequest(
                noticeId = "test",
                userQuery = message,
                stream = true,
                history = _messages.value
                    .filter { it.text != message }
                    .map { message ->
                        ChatHistory(
                            role = if (message.isUser) "user" else "system",
                            content = message.text
                        )
                    }
            )
            val result = org.openjwc.client.net.chat.sendMessage(chatRequest)

            when (result) {
                is NetworkResult.Success -> {
                    val botMessage = ChatMessage(
                        id = System.currentTimeMillis(),
                        text = result.content,
                        isUser = false
                    )
                    _messages.update { currentList ->
                        currentList + botMessage
                    }
                }

                is NetworkResult.ValidationError -> {
                    Log.e(label, "Validation error: ${result.errors}")
                }

                is NetworkResult.Failure -> {
                    Log.e(label, "Failure: ${result.code} ${result.msg}")
                }

                is NetworkResult.Error -> {
                    Log.e(label, "Error: ${result.msg}")
                }
            }
        }
        // TODO: 参数可能还得加一个会话 id，用来判断在哪一个会话聊天
    }
}