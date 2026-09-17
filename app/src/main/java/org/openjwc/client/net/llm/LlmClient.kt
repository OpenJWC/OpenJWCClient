package org.openjwc.client.net.llm

import kotlinx.coroutines.flow.Flow

/**
 * 供应商无关的流式聊天客户端。
 * 实现需自行处理鉴权、SSE 解析与 `tool_calls` 增量拼接。
 */
interface LlmClient {
    val config: LlmProviderConfig

    fun streamChat(
        messages: List<LlmMessage>,
        tools: List<LlmToolSpec> = emptyList(),
    ): Flow<LlmDelta>
}
