package org.openjwc.client.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.openjwc.client.data.dao.ChatDao
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.ChatSession
import org.openjwc.client.data.models.Role
import org.openjwc.client.data.repository.ChatStreamStatus.Failure
import org.openjwc.client.data.repository.ChatStreamStatus.Generating
import org.openjwc.client.data.repository.ChatStreamStatus.Loading
import org.openjwc.client.net.chat.sendMessageStream
import org.openjwc.client.net.models.ChatHistory
import org.openjwc.client.net.models.ChatNetworkResult
import org.openjwc.client.net.models.ChatRequestBody
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.net.models.NetClient
import kotlin.coroutines.cancellation.CancellationException

sealed class ChatStreamStatus {
    data object Loading : ChatStreamStatus()
    data object ToolCalling : ChatStreamStatus()
    data class Generating(val content: String) : ChatStreamStatus()
    data class Finished(val finalContent: String) : ChatStreamStatus()
    data class Failure(val code: Int, val msg: String) : ChatStreamStatus()
}

class ChatRepository(
    private val chatDao: ChatDao,
    private val settingsDataSource: SettingsDataSource,
    private val authDataSource: AuthDataSource
) {
    // 返回 session 的 metadata 里面的 id
    suspend fun createChatSession(title: String): Long {
        val metadata = ChatMetadata(title = title)
        return chatDao.insertMetadata(metadata)
    }

    // 返回 message 的 id
    private suspend fun insertMessage(chatMessage: ChatMessage): Long {
        val msgId = chatDao.insertMessage(chatMessage)
        chatDao.updateLastUpdated(chatMessage.ownerSessionId)
        return msgId
    }

    private suspend fun updateMessageText(messageId: Long, newText: String) =
        chatDao.updateMessageText(messageId, newText)

    suspend fun updateMetadata(metadata: ChatMetadata) = chatDao.updateMetadata(metadata)

    fun getChatSessions(): Flow<List<ChatSession>> = chatDao.getAllChatSessions()
    fun getChatMetadataById(id: Long): Flow<ChatMetadata?> =
        chatDao.getChatSessionById(id).map { it?.metadata }

    // 和上面那个函数作用类似，但是这个保证 ChatMessage 按照时间顺序排列
    fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessage>> =
        chatDao.getMessagesBySessionId(sessionId)

    suspend fun deleteMessageById(messageId: Long) = chatDao.deleteMessageById(messageId)
    suspend fun deleteSession(sessionId: Long) = chatDao.deleteSession(sessionId)
    /*
        suspend fun deleteAllSessions() = chatDao.deleteAllSessions()
    */

    private suspend fun handleFailure(aiMsgId: Long? = null) {
        aiMsgId?.let { deleteMessageById(it) }
    }

    fun sendMessage(
        sessionId: Long,
        messageText: String,
        attachments: List<FetchedNotice>,
    ) = flow {
        val currentSettings = settingsDataSource.userSettings.first()
        val authSession = authDataSource.authSession.first()
        if (!authSession.isLoggedIn || authSession.token == null) {
            emit(Failure(401, "Not Logged in"))
            return@flow
        }

        val attachmentTitles = attachments.map { it.title }
        val attachmentIds = attachments.map { it.id }

        insertMessage(
            ChatMessage(
                ownerSessionId = sessionId,
                text = messageText,
                role = Role.USER,
                attachmentTitles = attachmentTitles
            )
        )

        val aiMsgId = insertMessage(
            ChatMessage(
                ownerSessionId = sessionId,
                text = "",
                role = Role.ASSISTANT
            )
        )

        emit(Loading)

        try {
            val messages = getMessagesBySessionId(sessionId).first()
            val historyList = messages
                .filter { it.text.isNotBlank() }
                .map {
                    ChatHistory(
                        role = if (it.role == Role.USER) "user" else "assistant",
                        content = it.text
                    )
                }

            val apiService = NetClient.getService(
                currentSettings.host,
                currentSettings.port,
                currentSettings.useHttp,
                currentSettings.proxy
            )

            var currentFullText = ""
            var lastWriteTime = 0L

            apiService.sendMessageStream(
                authSession.token,
                authSession.uuid,
                ChatRequestBody(attachmentIds, messageText, true, historyList)
            ).collect { result ->
                when (result) {
                    // TODO: 目前只支持Generating状态，等后端写好tool calling 再补全
                    is ChatNetworkResult.Success -> {
                        currentFullText = result.content
                        emit(Generating(currentFullText))
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastWriteTime > 500L) {
                            updateMessageText(aiMsgId, currentFullText)
                            lastWriteTime = currentTime
                        }
                    }
                    is ChatNetworkResult.Failure -> {
                        handleFailure(aiMsgId)
                        emit(Failure(result.code, result.msg))
                        throw CancellationException("Network request failed")
                    }
                    is ChatNetworkResult.Error -> {
                        handleFailure(aiMsgId)
                        emit(Failure(-1, result.msg))
                        throw CancellationException("Network request failed")
                    }
                    else -> { /* 处理其他情况 */ }
                }
            }

            updateMessageText(aiMsgId, currentFullText)
            emit(ChatStreamStatus.Finished(currentFullText))

        } catch (e: Exception) {
            if (e !is CancellationException) {
                handleFailure(aiMsgId)
                emit(Failure(-1, e.localizedMessage ?: "Unknown Error"))
            }
        }
    }
}