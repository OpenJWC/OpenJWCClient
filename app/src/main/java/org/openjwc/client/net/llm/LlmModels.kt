package org.openjwc.client.net.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException

/**
 * LLM 供应商协议。绝大多数厂商兼容 [OPENAI] 的 `/chat/completions`，
 * [ANTHROPIC] / [GEMINI] 需要单独适配（后续阶段）。
 */
@Serializable
enum class LlmProtocol { OPENAI, ANTHROPIC, GEMINI }

/** 一个 provider 的配置。API Key 单独安全存储，不在这里。 */
@Serializable
data class LlmProviderConfig(
    val providerId: String = "openai",
    val protocol: LlmProtocol = LlmProtocol.OPENAI,
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4o-mini",
    val temperature: Float = 0.7f,
    @SerialName("max_tokens") val maxTokens: Int = 2048,
)

/** 供应商无关的对话消息。 */
@Serializable
data class LlmMessage(
    val role: String,
    val content: String = "",
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_calls") val toolCalls: List<LlmToolCall>? = null,
) {
    companion object {
        fun system(text: String) = LlmMessage(role = "system", content = text)
        fun user(text: String) = LlmMessage(role = "user", content = text)
        fun assistant(text: String) = LlmMessage(role = "assistant", content = text)
        fun tool(callId: String, text: String) =
            LlmMessage(role = "tool", content = text, toolCallId = callId)
    }
}

/** 模型请求的一次工具调用。 */
@Serializable
data class LlmToolCall(
    val id: String = "",
    val name: String = "",
    val arguments: String = "",
)

/** 提供给模型的工具描述。`parametersJson` 是 JSON Schema 字符串。 */
@Serializable
data class LlmToolSpec(
    val name: String,
    val description: String,
    @SerialName("parameters_json") val parametersJson: String,
)

/** 流式增量。 */
sealed interface LlmDelta {
    data class Text(val value: String) : LlmDelta
    data class ToolCallDelta(
        val index: Int,
        val id: String?,
        val name: String?,
        val argumentsChunk: String?,
    ) : LlmDelta

    data class Finished(val finishReason: String?) : LlmDelta
}

/** LLM HTTP 层错误。 */
class LlmHttpException(
    val status: Int,
    val responseText: String,
) : IOException("LLM HTTP $status")

/** 配置不完整（缺少 Key / baseUrl / model）。 */
class LlmConfigException(message: String) : IOException(message)
