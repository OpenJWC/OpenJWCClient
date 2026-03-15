package org.openjwc.client.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.openjwc.client.data.dao.ChatDao
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.ChatSession
class ChatRepository(private val chatDao: ChatDao) {
    // 返回 session 的 metadata 里面的 id
    suspend fun createChatSession(title: String): Long {
        val metadata = ChatMetadata(title = title)
        return chatDao.insertMetadata(metadata)
    }
    // 返回 message 的 id
    suspend fun sendMessage(chatMessage: ChatMessage): Long {
        val msgId = chatDao.insertMessage(chatMessage)
        chatDao.updateLastUpdated(chatMessage.ownerSessionId)
        return msgId
    }

    suspend fun updateMessageText(messageId: Long, newText: String) =
        chatDao.updateMessageText(messageId, newText)

    suspend fun updateMetadata(metadata: ChatMetadata) = chatDao.updateMetadata(metadata)

    fun getChatSessions(): Flow<List<ChatSession>> = chatDao.getAllChatSessions()

    fun getChatSessionById(id: Long): Flow<ChatSession?> = chatDao.getChatSessionById(id)

    fun getChatMetadataById(id: Long): Flow<ChatMetadata?> =
        chatDao.getChatSessionById(id).map { it?.metadata }

    // 和上面那个函数作用类似，但是这个保证 ChatMessage 按照时间顺序排列
    fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessage>> =
        chatDao.getMessagesBySessionId(sessionId)

    suspend fun deleteMessageById(messageId: Long) = chatDao.deleteMessageById(messageId)
    suspend fun deleteSession(sessionId: Long) = chatDao.deleteSession(sessionId)

    suspend fun deleteAllSessions() = chatDao.deleteAllSessions()
}