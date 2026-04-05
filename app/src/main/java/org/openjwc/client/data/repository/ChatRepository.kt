package org.openjwc.client.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.openjwc.client.data.dao.ChatDao
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.ChatSession
import org.openjwc.client.data.models.Role
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.net.chat.sendMessageStream
import org.openjwc.client.net.models.ChatHistory
import org.openjwc.client.net.models.ChatNetworkResult
import org.openjwc.client.net.models.ChatRequestBody
import org.openjwc.client.net.models.FetchedNotice
import org.openjwc.client.net.models.NetClient

class ChatRepository(private val chatDao: ChatDao) {
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

    // 太浪费，已弃用
    fun getChatSessionById(id: Long): Flow<ChatSession?> = chatDao.getChatSessionById(id)

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

    suspend fun sendMessage(
        sessionId: Long,
        messageText: String,
        attachments: List<FetchedNotice>,
        currentSettings: UserSettings,
    ) {
        var aiMsgId: Long? = null
        try {
            val messages = getMessagesBySessionId(sessionId).first()
            val attachmentIds = attachments.map { it.id }
            val attachmentTitles = attachments.map { it.title }
            insertMessage(
                ChatMessage(
                    ownerSessionId = sessionId,
                    text = messageText,
                    role = Role.USER,
                    attachmentTitles = attachmentTitles
                )
            )
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
            aiMsgId = insertMessage(
                ChatMessage(
                    ownerSessionId = sessionId,
                    text = "",
                    role = Role.ASSISTANT
                )
            )

            apiService.sendMessageStream(
                currentSettings.authKey, currentSettings.uuidString,
                ChatRequestBody(attachmentIds, messageText, true, historyList)
            ).collect { result ->
                when (result) {
                    is ChatNetworkResult.Success -> {
                        updateMessageText(aiMsgId, result.content)
                    }

                    is ChatNetworkResult.Failure -> throw Exception("请求失败(${result.code}): ${result.msg}")
                    is ChatNetworkResult.Error -> throw Exception(result.msg)
                    is ChatNetworkResult.ValidationError -> throw Exception("验证失败: ${result.errors.detail}")
                }
            }
        } catch (e: Exception) {
            handleFailure(aiMsgId)
            throw e
        }
    }
}