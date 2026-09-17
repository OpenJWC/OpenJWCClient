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
import org.openjwc.client.data.models.ChatToolCall
import org.openjwc.client.data.models.MessageStatus

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

    @Query("UPDATE chat_messages SET runId = :runId WHERE messageId = :messageId")
    suspend fun updateMessageRunId(messageId: Long, runId: String)

    /** 结束一轮回答：写入正文、状态、交付方式与失败 code。 */
    @Query(
        "UPDATE chat_messages SET text = :text, status = :status, delivery = :delivery, " +
            "errorCode = :errorCode WHERE messageId = :messageId"
    )
    suspend fun finishMessage(
        messageId: Long,
        text: String,
        status: MessageStatus,
        delivery: String?,
        errorCode: String?,
    )

    @Query("SELECT * FROM chat_messages WHERE ownerSessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessage>>

    @Query("DELETE FROM chat_messages WHERE messageId = :messageId")
    suspend fun deleteMessageById(messageId: Long)

    /* ================= 工具轨迹 ================= */

    @Insert
    suspend fun insertToolCall(call: ChatToolCall): Long

    @Query(
        "UPDATE chat_tool_calls SET status = :status, code = :code, durationMs = :durationMs " +
            "WHERE id = :id"
    )
    suspend fun completeToolCall(id: Long, status: String, code: String?, durationMs: Long?)

    @Query("SELECT * FROM chat_tool_calls WHERE messageId = :messageId ORDER BY position ASC")
    fun observeToolCalls(messageId: Long): Flow<List<ChatToolCall>>

    @Query(
        "SELECT * FROM chat_tool_calls WHERE messageId IN " +
            "(SELECT messageId FROM chat_messages WHERE ownerSessionId = :sessionId) " +
            "ORDER BY messageId ASC, position ASC"
    )
    fun observeToolCallsBySession(sessionId: Long): Flow<List<ChatToolCall>>

    /* ================= 会话 ================= */

    @Query("DELETE FROM chat_metadata WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM chat_metadata")
    suspend fun deleteAllSessions()
}
