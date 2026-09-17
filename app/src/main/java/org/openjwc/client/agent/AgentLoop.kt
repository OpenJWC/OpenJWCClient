package org.openjwc.client.agent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.openjwc.client.data.models.SourceEntity
import org.openjwc.client.data.repository.CorpusCatalog
import org.openjwc.client.data.repository.NoticeCorpus
import org.openjwc.client.log.Logger
import org.openjwc.client.net.llm.LlmClient
import org.openjwc.client.net.llm.LlmConfigException
import org.openjwc.client.net.llm.LlmDelta
import org.openjwc.client.net.llm.LlmHttpException
import org.openjwc.client.net.llm.LlmMessage
import org.openjwc.client.net.llm.LlmToolCall
import org.openjwc.client.net.llm.LlmToolSpec
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/** 整轮运行超出预算。 */
class AgentRunTimeoutException : RuntimeException("agent run timeout")

/** Agent 以失败终止（携带稳定 code 与安全摘要）。 */
class AgentRunFailedException(val code: String, val summary: String) : RuntimeException(summary)

/**
 * 本地 Agent 循环。
 *
 * 与后端 `internal/service/agent/loop.go` 同构：
 * 系统提示 + 元数据 + 历史 → 多轮「模型（带工具）→ 执行工具 → 观察」→
 * 最后用一次**禁用工具**的流式请求产出最终答案（工具轮正文不公开）。
 *
 * 与后端的差异（有意为之）：
 * - 工具是结构化工具（[AgentTools]），不是受限 Shell；
 * - 若整轮没有用过任何工具，直接把该轮内容作为最终答案，省掉一次模型调用。
 */
class AgentLoop(
    private val client: LlmClient,
    private val tools: AgentTools,
    private val repository: NoticeCorpus,
    private val budget: AgentBudget = AgentBudget(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    private val tag = "AgentLoop"

    fun run(request: AgentRequest): Flow<AgentEvent> = flow {
        val runId = UUID.randomUUID().toString().take(8)
        emit(AgentEvent.RunStarted(runId))
        try {
            execute(request, runId) { emit(it) }
        } catch (e: TimeoutCancellationException) {
            Logger.e(tag, "运行超时", e)
            emit(AgentEvent.RunFailed(runId, AgentFailure.TIMEOUT.code, AgentFailure.TIMEOUT.summary))
        } catch (e: AgentRunTimeoutException) {
            Logger.e(tag, "运行超时: ${e.message}")
            emit(AgentEvent.RunFailed(runId, AgentFailure.TIMEOUT.code, AgentFailure.TIMEOUT.summary))
        } catch (e: LlmConfigException) {
            Logger.e(tag, "模型未配置: ${e.message}")
            emit(AgentEvent.RunFailed(runId, AgentFailure.CONFIGURATION.code, AgentFailure.CONFIGURATION.summary))
        } catch (e: LlmHttpException) {
            val failure = AgentFailure.fromHttpStatus(e.status)
            Logger.e(tag, "模型 HTTP ${e.status} -> ${failure.code}")
            emit(AgentEvent.RunFailed(runId, failure.code, failure.summary))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(tag, "运行失败: ${e.message}", e)
            emit(AgentEvent.RunFailed(runId, AgentFailure.FAILED.code, AgentFailure.FAILED.summary))
        }
    }

    /**
     * 便捷入口：只收集最终答案（日报这类批量任务用）。
     * 失败时抛出 [AgentRunFailedException]。
     */
    suspend fun answer(request: AgentRequest): String {
        val builder = StringBuilder()
        var failure: AgentEvent.RunFailed? = null
        run(request).collect { event ->
            when (event) {
                is AgentEvent.AnswerDelta -> builder.append(event.text)
                is AgentEvent.RunFailed -> failure = event
                else -> Unit
            }
        }
        failure?.let { throw AgentRunFailedException(it.code, it.summary) }
        return builder.toString()
    }

    private suspend fun execute(
        request: AgentRequest,
        runId: String,
        emit: suspend (AgentEvent) -> Unit,
    ) {
        val deadline = System.currentTimeMillis() + budget.runTimeoutMs
        Logger.d(
            tag,
            "运行开始 run=$runId queryLen=${request.query.length} history=${request.history.size} " +
                "noticeIds=${request.noticeIds.size}",
        )
        val referencedIds = (request.noticeIds + request.history.flatMap { it.attachmentIds }).distinct()
        val messages = mutableListOf<LlmMessage>()
        messages += LlmMessage.system(systemPrompt(referencedIds))
        messages += request.history
            .filter { it.role == "user" || it.role == "assistant" }
            .map { LlmMessage(it.role, it.content) }
        messages += LlmMessage.user(PromptTemplates.userQuery(request))

        var rounds = 0
        var toolCalls = 0
        var totalBytes = 0

        while (rounds < budget.maxModelRounds) {
            ensureRemaining(deadline)
            rounds++
            val round = collectRound(messages, useTools = true)

            if (round.calls.isEmpty()) {
                // 工具轮的正文不公开（可能夹带思考/后续还会声明工具调用）：
                // 统一再发一次「禁用工具」的流式请求产出最终答案（对齐后端）
                if (round.content.isNotBlank()) {
                    messages += LlmMessage.assistant(round.content)
                }
                finalize(messages, PromptTemplates.finalInstruction(), emit)
                Logger.d(tag, "运行完成 run=$runId rounds=$rounds tools=$toolCalls bytes=$totalBytes")
                emit(AgentEvent.RunCompleted(runId))
                return
            }

            messages += LlmMessage(role = "assistant", content = round.content, toolCalls = round.calls)

            if (round.calls.size > budget.maxToolsPerRound) {
                appendSkippedObservations(messages, round.calls, REASON_ROUND_LIMIT)
                finalize(messages, PromptTemplates.limitInstruction(REASON_ROUND_LIMIT), emit)
                emit(AgentEvent.RunCompleted(runId))
                return
            }

            for ((index, call) in round.calls.withIndex()) {
                if (toolCalls >= budget.maxToolCalls) {
                    appendSkippedObservations(messages, round.calls.drop(index), REASON_TOOL_LIMIT)
                    finalize(messages, PromptTemplates.limitInstruction(REASON_TOOL_LIMIT), emit)
                    emit(AgentEvent.RunCompleted(runId))
                    return
                }
                toolCalls++
                totalBytes += observe(call, toolCalls, messages, emit)
                if (totalBytes > budget.maxTotalToolBytes) {
                    appendSkippedObservations(messages, round.calls.drop(index + 1), REASON_OUTPUT_LIMIT)
                    finalize(messages, PromptTemplates.limitInstruction(REASON_OUTPUT_LIMIT), emit)
                    emit(AgentEvent.RunCompleted(runId))
                    return
                }
            }
        }

        Logger.w(tag, "运行收束 run=$runId 原因=$REASON_ROUND_LIMIT rounds=$rounds tools=$toolCalls")
        finalize(messages, PromptTemplates.limitInstruction(REASON_ROUND_LIMIT), emit)
        emit(AgentEvent.RunCompleted(runId))
    }

    /** 执行一次工具调用，发送配对事件，并把截断后的观察写入 [messages]。 */
    private suspend fun observe(
        call: LlmToolCall,
        index: Int,
        messages: MutableList<LlmMessage>,
        emit: suspend (AgentEvent) -> Unit,
    ): Int {
        val toolId = "tool-$index"
        val summary = if (tools.supports(call.name)) {
            tools.summarize(call.name, call.arguments)
        } else {
            "未知工具"
        }
        emit(AgentEvent.ToolStarted(toolId, call.name, summary, targetId = tools.targetId(call.name, call.arguments)))

        val started = System.currentTimeMillis()
        var status: String
        var code: String? = null
        var output: String
        try {
            output = tools.execute(call.name, call.arguments)
            status = AgentEvent.STATUS_COMPLETED
        } catch (e: AgentToolException) {
            status = AgentEvent.STATUS_FAILED
            code = e.code
            output = "工具执行失败（${e.code}）：${e.message}"
        } catch (e: Exception) {
            status = AgentEvent.STATUS_FAILED
            code = "tool_failed"
            output = "工具执行失败：${e.localizedMessage ?: "未知错误"}"
        }

        emit(
            AgentEvent.ToolCompleted(
                toolId = toolId,
                name = call.name,
                status = status,
                durationMs = System.currentTimeMillis() - started,
                code = code,
            )
        )

        val clipped = clipUtf8(output, budget.maxToolResultBytes)
        Logger.d(
            tag,
            "工具 $toolId ${call.name} $status ${System.currentTimeMillis() - started}ms " +
                "out=${clipped.length}${code?.let { " code=$it" } ?: ""}",
        )
        messages += LlmMessage.tool(call.id, clipped)
        return clipped.toByteArray(Charsets.UTF_8).size
    }

    /** 收束：追加指令后做一次禁用工具的流式请求。 */
    private suspend fun finalize(
        messages: MutableList<LlmMessage>,
        instruction: String,
        emit: suspend (AgentEvent) -> Unit,
    ) {
        messages += LlmMessage.user(instruction)
        var streamed = false
        val round = collectRound(messages, useTools = false) { text ->
            streamed = true
            emit(AgentEvent.AnswerDelta(text, AgentEvent.DELIVERY_STREAMING))
        }
        if (!streamed && round.content.isNotBlank()) {
            emit(AgentEvent.AnswerDelta(round.content, AgentEvent.DELIVERY_BUFFERED))
        }
    }

    private data class ModelRound(val content: String, val calls: List<LlmToolCall>)

    /** 完成一次模型请求，累积正文与跨帧的 tool_calls 增量。 */
    private suspend fun collectRound(
        messages: List<LlmMessage>,
        useTools: Boolean,
        onText: (suspend (String) -> Unit)? = null,
    ): ModelRound {
        val ids = mutableMapOf<Int, StringBuilder>()
        val names = mutableMapOf<Int, StringBuilder>()
        val args = mutableMapOf<Int, StringBuilder>()
        val content = StringBuilder()

        withTimeout(budget.modelTimeoutMs.milliseconds) {
            client.streamChat(messages, if (useTools) tools.specs.toLlmSpecs() else emptyList())
                .collect { delta ->
                    when (delta) {
                        is LlmDelta.Text -> {
                            content.append(delta.value)
                            onText?.invoke(delta.value)
                        }

                        is LlmDelta.ToolCallDelta -> {
                            delta.id?.let { ids.getOrPut(delta.index) { StringBuilder() }.append(it) }
                            delta.name?.let { names.getOrPut(delta.index) { StringBuilder() }.append(it) }
                            delta.argumentsChunk?.let {
                                args.getOrPut(delta.index) { StringBuilder() }.append(it)
                            }
                        }

                        is LlmDelta.Finished -> Unit
                    }
                }
        }

        val indexes = (ids.keys + names.keys + args.keys).toSortedSet()
        val calls = indexes.map { index ->
            LlmToolCall(
                id = ids[index]?.toString().orEmpty().ifBlank { "call_$index" },
                name = names[index]?.toString().orEmpty(),
                arguments = args[index]?.toString().orEmpty().ifBlank { "{}" },
            )
        }
        return ModelRound(content.toString(), calls)
    }

    private suspend fun systemPrompt(referencedIds: List<String>): String {
        val catalog = runCatching { repository.corpusCatalog() }
            .getOrElse { CorpusCatalog(0, null, null) }
        val sources = runCatching { repository.subscribedSources() }.getOrDefault(emptyList())
        val lastCrawl = sources.mapNotNull { it.lastRunAt }.maxOrNull()
        val base = PromptTemplates.SYSTEM_PROMPT + "\n" + PromptTemplates.TOOL_INSTRUCTIONS + "\n" +
            PromptTemplates.metadata(
                catalog = catalog,
                lastCrawlMillis = lastCrawl,
                now = ZonedDateTime.now(zoneId),
                sources = sources.map { it.id to it.name },
            )
        val referenced = referencedNoticeBlock(referencedIds, sources)
        return if (referenced == null) base else base + "\n" + referenced
    }

    /**
     * 把本会话引用过的资讯（当前 + 历史，去重）汇总成一段 system 文本。
     * 除了元数据，还内联一段正文节选：这样即使模型不主动调 read_notice，也能直接据此回答。
     * 超出预算的部分只保留元数据/省略，需要全文时再用 read_notice(id)。
     */
    private suspend fun referencedNoticeBlock(
        ids: List<String>,
        sources: List<SourceEntity>,
    ): String? {
        if (ids.isEmpty()) return null
        val names = sources.associate { it.id to it.name }
        val sb = StringBuilder()
        var used = 0
        for (id in ids.take(MAX_REFERENCED_NOTICES)) {
            val notice = runCatching { repository.findNotice(id) }.getOrNull()
            val head = if (notice == null) {
                "- id=$id（本地未找到）"
            } else {
                buildString {
                    append("- ").append(notice.title)
                    append("（").append(notice.publishedDay)
                    notice.sourceId?.let { names[it] }?.takeIf { it.isNotBlank() }?.let {
                        append("，").append(it)
                    }
                    append("，id=").append(notice.id).append("）")
                }
            }
            val excerpt = notice?.content?.trim().orEmpty().take(REFERENCED_EXCERPT_CHARS)
            val block = if (excerpt.isBlank()) {
                head
            } else {
                head + "\n" + excerpt
            }
            if (used + block.length > MAX_REFERENCED_CHARS) break
            sb.append("\n\n").append(block)
            used += block.length
        }
        if (sb.isEmpty()) return null
        return "【引用的资讯】用户在本会话中引用了以下资讯（含正文节选），回答时应优先依据这些内容；" +
            "需要完整正文再用 read_notice(id)：" + sb
    }

    private fun ensureRemaining(deadline: Long) {
        if (System.currentTimeMillis() >= deadline) throw AgentRunTimeoutException()
    }

    private fun appendSkippedObservations(
        messages: MutableList<LlmMessage>,
        calls: List<LlmToolCall>,
        reason: String,
    ) {
        calls.forEach { call ->
            messages += LlmMessage.tool(call.id, "该工具调用未执行：检索因 $reason 停止。")
        }
    }

    private fun List<AgentToolSpec>.toLlmSpecs(): List<LlmToolSpec> = map {
        LlmToolSpec(name = it.name, description = it.description, parametersJson = it.parameters.toString())
    }

    private companion object {
        const val REASON_ROUND_LIMIT = "round_limit"
        const val REASON_TOOL_LIMIT = "tool_limit"
        const val REASON_OUTPUT_LIMIT = "tool_output_limit"

        /** system「引用的资讯」块：最多列几条、单条正文节选多少字符、整块多少字符。 */
        private const val MAX_REFERENCED_NOTICES = 5
        private const val REFERENCED_EXCERPT_CHARS = 1_500
        private const val MAX_REFERENCED_CHARS = 6_000

        const val TRUNCATED_SUFFIX = "\n[内容已截断]"

        /** 按 UTF-8 字节截断，不破坏字符编码。 */
        fun clipUtf8(text: String, limit: Int): String {
            val bytes = text.toByteArray(Charsets.UTF_8)
            if (bytes.size <= limit) return text
            if (limit <= TRUNCATED_SUFFIX.length) return TRUNCATED_SUFFIX.substring(0, limit)
            var cut = limit - TRUNCATED_SUFFIX.length
            while (cut > 0 && (bytes[cut].toInt() and 0xC0) == 0x80) cut--
            return String(bytes, 0, cut, Charsets.UTF_8) + TRUNCATED_SUFFIX
        }
    }
}
