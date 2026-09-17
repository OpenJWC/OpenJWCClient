package org.openjwc.client.agent

import kotlinx.serialization.json.JsonObject
import org.openjwc.client.net.llm.LlmToolCall

/**
 * 提供给模型的工具描述。参数是 JSON Schema。
 */
data class AgentToolSpec(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

/** 一次 Agent 运行的输入。 */
data class AgentRequest(
    /** 用户问题，或日报这类批量任务的任务描述。 */
    val query: String,
    /** 只允许 user / assistant 的已完成历史。 */
    val history: List<AgentMessage> = emptyList(),
    /** 用户显式选中的资讯，会作为证据提示拼进问题。 */
    val noticeIds: List<String> = emptyList(),
)

/** 对话消息（工具轮会带 toolCalls，工具结果带 toolCallId）。 */
data class AgentMessage(
    val role: String,
    val content: String = "",
    val toolCalls: List<LlmToolCall> = emptyList(),
    val toolCallId: String? = null,
    /** 该条用户消息引用过的资讯 id（用于汇总到 system 的「引用资讯」块）。 */
    val attachmentIds: List<String> = emptyList(),
) {
    companion object {
        fun user(text: String) = AgentMessage(role = "user", content = text)
        fun assistant(text: String) = AgentMessage(role = "assistant", content = text)
    }
}

/** 运行期间对外产出的事件；语义与后端 Chat v2 对齐，便于复用 UI 归约。 */
sealed interface AgentEvent {
    data class RunStarted(val runId: String) : AgentEvent
    data class ToolStarted(
        val toolId: String,
        val name: String,
        val summary: String,
        /** 工具指向的本地对象 id（如 read_notice 的资讯 id），可空。 */
        val targetId: String? = null,
    ) : AgentEvent
    data class ToolCompleted(
        val toolId: String,
        val name: String,
        val status: String,
        val durationMs: Long,
        val code: String?,
    ) : AgentEvent

    data class AnswerDelta(val text: String, val delivery: String) : AgentEvent
    data class RunCompleted(val runId: String) : AgentEvent
    data class RunFailed(val runId: String, val code: String, val summary: String) : AgentEvent

    companion object {
        const val DELIVERY_STREAMING = "streaming-final"
        const val DELIVERY_BUFFERED = "buffered-final"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"
    }
}

/** 运行失败分类，用于给出稳定 code 与安全摘要。 */
enum class AgentFailure(val code: String, val summary: String) {
    TIMEOUT("agent_timeout", "问答超时，请重试或缩小问题范围"),
    MODEL_UNAVAILABLE("model_unavailable", "模型服务暂不可用，请稍后重试"),
    /** 401 / 403：Key 无效或没有权限。 */
    MODEL_AUTH("agent_auth_error", "API Key 无效或没有权限，请到「AI 模型设置」检查"),
    /** 404：模型名或 Base URL 不对。 */
    MODEL_NOT_FOUND("agent_model_not_found", "模型名称或接口地址不正确，请到「AI 模型设置」检查"),
    /** 429：限流或额度不足。 */
    MODEL_RATE_LIMITED("agent_rate_limited", "请求过于频繁或额度不足，请稍后重试"),
    MODEL_PROTOCOL("model_protocol_error", "模型响应异常，请重试"),
    CONFIGURATION("agent_configuration_error", "尚未配置可用的模型，请先在设置中填写"),
    CANCELLED("agent_cancelled", "问答已取消"),
    FAILED("agent_failed", "问答未完成，请重试");

    companion object {
        /** 需要用户去「AI 模型设置」修正的失败（聊天页会直接跳过去）。 */
        val CONFIG_RELATED: Set<String> = setOf(
            CONFIGURATION.code,
            MODEL_AUTH.code,
            MODEL_NOT_FOUND.code,
        )

        /** 按 HTTP 状态码归类。 */
        fun fromHttpStatus(status: Int): AgentFailure = when (status) {
            401, 403 -> MODEL_AUTH
            404 -> MODEL_NOT_FOUND
            429 -> MODEL_RATE_LIMITED
            else -> MODEL_UNAVAILABLE
        }
    }
}
