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
    val timestamp: Long = System.currentTimeMillis()
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