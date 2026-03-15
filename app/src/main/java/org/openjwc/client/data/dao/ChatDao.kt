package org.openjwc.client.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.ChatSession
@Dao
interface ChatDao {
    @Transaction
    @Query("SELECT * FROM chat_metadata ORDER BY lastUpdated DESC")
    fun getAllChatSessions(): Flow<List<ChatSession>>

    @Transaction
    @Query("SELECT * FROM chat_metadata WHERE sessionId = :id")
    fun getChatSessionById(id: Long): Flow<ChatSession?>

    @Insert
    suspend fun insertMetadata(metadata: ChatMetadata): Long

    @Update
    suspend fun updateMetadata(metadata: ChatMetadata)

    @Query("UPDATE chat_metadata SET lastUpdated = :timestamp WHERE sessionId = :sessionId")
    suspend fun updateLastUpdated(sessionId: Long, timestamp: Long = System.currentTimeMillis())

    @Insert
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("UPDATE chat_messages SET text = :newText WHERE messageId = :messageId")
    suspend fun updateMessageText(messageId: Long, newText: String)

    @Query("SELECT * FROM chat_messages WHERE ownerSessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessage>>
    @Query("DELETE FROM chat_messages WHERE messageId = :messageId")
    suspend fun deleteMessageById(messageId: Long)

    @Query("DELETE FROM chat_metadata WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM chat_metadata")
    suspend fun deleteAllSessions()
}