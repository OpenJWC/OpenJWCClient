package org.openjwc.client.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.openjwc.client.agent.AgentEvent
import org.openjwc.client.agent.AgentLoopFactory
import org.openjwc.client.agent.AgentMessage
import org.openjwc.client.agent.AgentRequest
import org.openjwc.client.data.dao.ChatDao
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.ChatSession
import org.openjwc.client.data.models.ChatToolCall
import org.openjwc.client.data.models.ChatTurn
import org.openjwc.client.data.models.MessageStatus
import org.openjwc.client.data.models.Role
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.FetchedNotice

/** 工具卡片 UI 状态。 */
data class ToolUiState(
    val id: String,
    val name: String,
    val summary: String,
    val status: String = STATUS_RUNNING,
    val durationMs: Long? = null,
    val code: String? = null,
    /** 工具指向的本地对象 id（read_notice 的资讯 id），可点击进入详情。 */
    val targetId: String? = null,
) {
    companion object {
        const val STATUS_RUNNING = "running"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"
    }
}

fun ChatToolCall.toUiState() = ToolUiState(
    id = id.toString(),
    name = name,
    summary = summary,
    status = status,
    durationMs = durationMs,
    code = code,
    targetId = targetId,
)

/** 一轮回答的流式状态；工具卡片本身由数据库驱动。 */
sealed class ChatStreamStatus {
    data object Loading : ChatStreamStatus()
    data object ToolRunning : ChatStreamStatus()
    data class Generating(val content: String) : ChatStreamStatus()
    data class Finished(val finalContent: String) : ChatStreamStatus()
    data class Failure(
        val code: Int,
        val msg: String,
        /** Agent 的稳定失败 code（如 `agent_configuration_error`）。 */
        val errorCode: String? = null,
    ) : ChatStreamStatus()
}

/**
 * 本地对话仓库：直接用用户自带 API Key 驱动 [AgentLoop]，不再经过自建服务端。
 * 工具轨迹随消息落库，重启后仍可还原。
 */
class ChatRepository(
    private val chatDao: ChatDao,
    private val agentLoopFactory: AgentLoopFactory,
) {
    private val tag = "ChatRepository"

    // 返回 session 的 metadata 里面的 id
    suspend fun createChatSession(title: String): Long {
        val metadata = ChatMetadata(title = title)
        return chatDao.insertMetadata(metadata)
    }

    private suspend fun insertMessage(chatMessage: ChatMessage): Long {
        val msgId = chatDao.insertMessage(chatMessage)
        chatDao.updateLastUpdated(chatMessage.ownerSessionId)
        return msgId
    }

    suspend fun updateMetadata(metadata: ChatMetadata) = chatDao.updateMetadata(metadata)

    fun getChatSessions(): Flow<List<ChatSession>> = chatDao.getAllChatSessions()
    fun getChatMetadataById(id: Long): Flow<ChatMetadata?> =
        chatDao.getChatSessionById(id).map { it?.metadata }

    fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessage>> =
        chatDao.getMessagesBySessionId(sessionId)

    /** 消息 + 工具轨迹（工具卡片直接来自数据库）。 */
    fun observeTurns(sessionId: Long): Flow<List<ChatTurn>> = combine(
        chatDao.getMessagesBySessionId(sessionId),
        chatDao.observeToolCallsBySession(sessionId),
    ) { messages, tools ->
        val byMessage = tools.groupBy { it.messageId }
        messages.map { ChatTurn(it, byMessage[it.messageId].orEmpty()) }
    }

    suspend fun deleteMessageById(messageId: Long) = chatDao.deleteMessageById(messageId)
    suspend fun deleteSession(sessionId: Long) = chatDao.deleteSession(sessionId)

    /**
     * 一轮本地 Agent 问答：
     * 1) 落库用户消息与占位助手消息；
     * 2) 用 [AgentLoop] 跑工具循环，工具轨迹逐条落库；
     * 3) 完成/失败时写回助手消息状态（失败保留部分正文与工具轨迹）。
     */
    fun sendMessage(
        sessionId: Long,
        messageText: String,
        attachments: List<FetchedNotice>,
        isRetry: Boolean = false,
    ): Flow<ChatStreamStatus> = flow {
        // 重试时不重复插入用户消息，直接复用已有的那一条
        val userMsgId = if (isRetry) {
            null
        } else {
            insertMessage(
                ChatMessage(
                    ownerSessionId = sessionId,
                    text = messageText,
                    role = Role.USER,
                    attachmentTitles = attachments.map { it.title },
                    attachmentIds = attachments.map { it.id }
                )
            )
        }

        val assistantId = insertMessage(
            ChatMessage(
                ownerSessionId = sessionId,
                text = "",
                role = Role.ASSISTANT,
                status = MessageStatus.RUNNING
            )
        )

        emit(ChatStreamStatus.Loading)

        var fullText = ""
        var delivery: String? = null
        var toolCount = 0
        var terminal = false
        val toolRowIds = mutableMapOf<String, Long>()

        try {
            val loop = agentLoopFactory.create()
            Logger.d(tag, "sendMessage session=$sessionId attachments=${attachments.size}")

            val history = buildHistory(sessionId, userMsgId)

            loop.run(
                AgentRequest(
                    query = messageText,
                    history = history,
                    noticeIds = attachments.map { it.id },
                )
            ).collect { event ->
                when (event) {
                    is AgentEvent.RunStarted -> {
                        chatDao.updateMessageRunId(assistantId, event.runId)
                        emit(ChatStreamStatus.Loading)
                    }

                    is AgentEvent.ToolStarted -> {
                        val rowId = chatDao.insertToolCall(
                            ChatToolCall(
                                messageId = assistantId,
                                position = toolCount++,
                                name = event.name,
                                summary = event.summary,
                                status = ToolUiState.STATUS_RUNNING,
                                targetId = event.targetId,
                            )
                        )
                        toolRowIds[event.toolId] = rowId
                        emit(ChatStreamStatus.ToolRunning)
                    }

                    is AgentEvent.ToolCompleted -> {
                        toolRowIds[event.toolId]?.let { rowId ->
                            chatDao.completeToolCall(rowId, event.status, event.code, event.durationMs)
                        }
                        emit(ChatStreamStatus.ToolRunning)
                    }

                    is AgentEvent.AnswerDelta -> {
                        fullText += event.text
                        delivery = event.delivery
                        emit(ChatStreamStatus.Generating(fullText))
                    }

                    is AgentEvent.RunCompleted -> {
                        chatDao.finishMessage(
                            assistantId, fullText, MessageStatus.COMPLETED, delivery, null
                        )
                        terminal = true
                        emit(ChatStreamStatus.Finished(fullText))
                    }

                    is AgentEvent.RunFailed -> {
                        chatDao.finishMessage(
                            assistantId, fullText, MessageStatus.FAILED, delivery, event.code
                        )
                        terminal = true
                        emit(ChatStreamStatus.Failure(-1, event.summary, event.code))
                    }
                }
            }

            if (!terminal) {
                chatDao.finishMessage(
                    assistantId, fullText, MessageStatus.FAILED, delivery, "agent_interrupted"
                )
                emit(ChatStreamStatus.Failure(-1, "回答中断，请重试"))
            }
        } catch (e: CancellationException) {
            chatDao.finishMessage(
                assistantId, fullText, MessageStatus.FAILED, delivery, "agent_cancelled"
            )
            throw e
        } catch (e: Exception) {
            Logger.e(tag, "sendMessage Error: ${e.message}", e)
            chatDao.finishMessage(
                assistantId, fullText, MessageStatus.FAILED, delivery, "agent_failed"
            )
            if (fullText.isBlank() && toolCount == 0) {
                chatDao.deleteMessageById(assistantId)
            }
            emit(ChatStreamStatus.Failure(-1, e.localizedMessage ?: "问答失败，请重试"))
        }

        // 失败且没有任何产出时清掉空壳助手消息
        if (fullText.isBlank() && toolCount == 0) {
            chatDao.deleteMessageById(assistantId)
        }
    }

    /**
     * 只回放已完成的 user/assistant 文本：最多 20 条、总量约 48KB。
     * 当前问题通过 `query` 单独发送，历史里的附件不再伪装成消息。
     */
    private suspend fun buildHistory(sessionId: Long, currentUserMsgId: Long?): List<AgentMessage> {
        val maxTotalBytes = 48 * 1024
        val messages = chatDao.getMessagesBySessionId(sessionId).first()
        val picked = ArrayDeque<AgentMessage>()
        var totalBytes = 0

        val candidates = messages.filter {
            it.status == MessageStatus.COMPLETED &&
                it.text.isNotBlank() &&
                it.messageId != currentUserMsgId
        }
        for (message in candidates.asReversed()) {
            if (picked.size >= MAX_HISTORY_MESSAGES) break
            val bytes = message.text.toByteArray(Charsets.UTF_8).size
            if (totalBytes + bytes > maxTotalBytes) break
            totalBytes += bytes
            picked.addFirst(
                AgentMessage(
                    role = if (message.role == Role.USER) "user" else "assistant",
                    content = message.text,
                    attachmentIds = message.attachmentIds,
                )
            )
        }
        return picked.toList()
    }

    private companion object {
        const val MAX_HISTORY_MESSAGES = 20
    }
}
