package org.openjwc.client.net.llm

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 按配置构造 [LlmClient]。
 * 这里使用**独立**的 OkHttpClient：不挂 `NetClient.loggingInterceptor`
 * （它是 `Level.HEADERS`，会把用户的 Authorization 打进日志）。
 */
object LlmClientFactory {

    @Volatile
    private var sharedClient: OkHttpClient? = null

    private fun client(): OkHttpClient = sharedClient ?: synchronized(this) {
        sharedClient ?: OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // 流式：读超时关闭，整体上限放宽
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
            .also { sharedClient = it }
    }

    fun create(config: LlmProviderConfig, apiKey: String): LlmClient = when (config.protocol) {
        // Anthropic / Gemini 暂未适配，先走 OpenAI 兼容路径
        LlmProtocol.OPENAI, LlmProtocol.ANTHROPIC, LlmProtocol.GEMINI ->
            OpenAiCompatibleClient(client(), config, apiKey)
    }
}
