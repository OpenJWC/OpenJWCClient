package org.openjwc.client.net.llm

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.openjwc.client.log.Logger
import java.io.IOException

private const val LABEL = "LlmOpenAI"

/**
 * 兼容 OpenAI `/chat/completions` 的流式客户端。
 * 覆盖 OpenAI / DeepSeek / Moonshot / GLM / Qwen(DashScope) / OpenRouter / Groq / Ollama 等。
 */
class OpenAiCompatibleClient(
    private val client: OkHttpClient,
    override val config: LlmProviderConfig,
    private val apiKey: String,
) : LlmClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun buildPayload(
        messages: List<LlmMessage>,
        tools: List<LlmToolSpec>,
    ): String = buildJsonObject {
        put("model", config.model)
        put("stream", true)
        put("temperature", config.temperature)
        put("max_tokens", config.maxTokens)
        putJsonArray("messages") {
            messages.forEach { message ->
                addJsonObject {
                    put("role", message.role)
                    message.toolCallId?.let { put("tool_call_id", it) }
                    val calls = message.toolCalls
                    if (calls.isNullOrEmpty()) {
                        put("content", message.content)
                    } else {
                        putJsonArray("tool_calls") {
                            calls.forEach { call ->
                                addJsonObject {
                                    put("id", call.id)
                                    put("type", "function")
                                    putJsonObject("function") {
                                        put("name", call.name)
                                        put("arguments", call.arguments)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (tools.isNotEmpty()) {
            putJsonArray("tools") {
                tools.forEach { tool ->
                    addJsonObject {
                        put("type", "function")
                        putJsonObject("function") {
                            put("name", tool.name)
                            put("description", tool.description)
                            put(
                                "parameters",
                                runCatching { json.parseToJsonElement(tool.parametersJson) }
                                    .getOrElse { Json.parseToJsonElement("""{"type":"object"}""") },
                            )
                        }
                    }
                }
            }
        }
    }.toString()

    override fun streamChat(
        messages: List<LlmMessage>,
        tools: List<LlmToolSpec>,
    ): Flow<LlmDelta> = callbackFlow {
        if (apiKey.isBlank()) {
            close(LlmConfigException("缺少 API Key"))
            return@callbackFlow
        }
        if (config.baseUrl.isBlank() || config.model.isBlank()) {
            close(LlmConfigException("缺少 baseUrl 或 model"))
            return@callbackFlow
        }

        val url = config.baseUrl.trimEnd('/') + "/chat/completions"
        val body = buildPayload(messages, tools).toRequestBody(jsonType)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        val call = client.newCall(request)
        Logger.d(LABEL, "POST $url model=${config.model} messages=${messages.size} tools=${tools.size}")

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                Logger.e(LABEL, "onFailure: ${error.localizedMessage}", error)
                close(error)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body
                    if (!response.isSuccessful) {
                        val text = responseBody?.string()?.take(4096).orEmpty()
                        Logger.e(LABEL, "HTTP ${response.code}: ${text.take(256)}")
                        close(LlmHttpException(response.code, text))
                        return
                    }
                    if (responseBody == null) {
                        close(IOException("Empty response body"))
                        return
                    }

                    var finished = false
                    try {
                        val source = responseBody.source()
                        while (true) {
                            val line = source.readUtf8Line() ?: break
                            if (line.isEmpty() || line.startsWith(":")) continue
                            if (!line.startsWith("data:")) continue
                            val data = line.removePrefix("data:").trim()
                            if (data.isEmpty()) continue
                            if (data == "[DONE]") {
                                finished = true
                                break
                            }
                            val deltas = parseChunk(data)
                            for (delta in deltas) {
                                if (trySendBlocking(delta).isFailure) {
                                    call.cancel()
                                    return
                                }
                                if (delta is LlmDelta.Finished) finished = true
                            }
                        }
                        if (!finished) {
                            trySendBlocking(LlmDelta.Finished(null))
                        }
                        close()
                    } catch (error: Throwable) {
                        Logger.e(LABEL, "stream error: ${error.localizedMessage}", error)
                        close(error)
                    }
                }
            }
        })

        awaitClose { call.cancel() }
    }.buffer(capacity = 16)

    private fun parseChunk(data: String): List<LlmDelta> {
        val root = runCatching { json.parseToJsonElement(data).jsonObject }.getOrNull()
            ?: return emptyList()
        val choices = root["choices"]?.jsonArray ?: return emptyList()
        val choice = choices.firstOrNull()?.jsonObject ?: return emptyList()
        val deltas = mutableListOf<LlmDelta>()

        choice["delta"]?.jsonObject?.let { delta ->
            delta["content"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotEmpty() }
                ?.let { deltas += LlmDelta.Text(it) }

            delta["tool_calls"]?.jsonArray?.forEach { element ->
                val call = element.jsonObject
                val index = call["index"]?.jsonPrimitive?.intOrNull ?: 0
                val function = call["function"]?.jsonObject
                deltas += LlmDelta.ToolCallDelta(
                    index = index,
                    id = call["id"]?.jsonPrimitive?.contentOrNull,
                    name = function?.get("name")?.jsonPrimitive?.contentOrNull,
                    argumentsChunk = function?.get("arguments")?.jsonPrimitive?.contentOrNull,
                )
            }
        }

        choice["finish_reason"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotEmpty() && it != "null" }
            ?.let { deltas += LlmDelta.Finished(it) }

        return deltas
    }
}
