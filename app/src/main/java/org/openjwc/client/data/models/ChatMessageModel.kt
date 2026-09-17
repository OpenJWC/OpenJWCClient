package org.openjwc.client.data.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class Role {
    USER,
    ASSISTANT
}

/** 一轮回答的落库状态；只有 [COMPLETED] 会进入下一轮 history。 */
enum class MessageStatus {
    RUNNING,
    COMPLETED,
    FAILED
}

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatMetadata::class,
            parentColumns = ["sessionId"],
            childColumns = ["ownerSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ownerSessionId"])]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
    val ownerSessionId: Long,
    val text: String,
    val role: Role,
    val attachmentTitles: List<String> = emptyList(),
    val attachmentIds: List<String> = emptyList(),
    val status: MessageStatus = MessageStatus.COMPLETED,
    /** 本轮 Agent 运行的标识（助手消息）。 */
    val runId: String? = null,
    /** `streaming-final` / `buffered-final`。 */
    val delivery: String? = null,
    /** 失败时的稳定 code（如 `agent_timeout`）。 */
    val errorCode: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/** 助手消息里的单次工具调用，用于重启后还原工具卡片。 */
@Entity(
    tableName = "chat_tool_calls",
    foreignKeys = [
        ForeignKey(
            entity = ChatMessage::class,
            parentColumns = ["messageId"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["messageId"])]
)
data class ChatToolCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long,
    val position: Int,
    val name: String,
    val summary: String,
    val status: String,
    val code: String? = null,
    val durationMs: Long? = null,
    /** 工具指向的本地对象 id（目前是 read_notice 的资讯 id，便于点击进入详情）。 */
    val targetId: String? = null
)

/** 一条消息及其工具轨迹。 */
data class ChatTurn(
    val message: ChatMessage,
    val toolCalls: List<ChatToolCall> = emptyList()
)

@Entity(tableName = "chat_metadata")
data class ChatMetadata(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val title: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ChatSession(
    @Embedded val metadata: ChatMetadata,
    @Relation(
        parentColumn = "sessionId",
        entityColumn = "ownerSessionId"
    )
    val messages: List<ChatMessage>
)
