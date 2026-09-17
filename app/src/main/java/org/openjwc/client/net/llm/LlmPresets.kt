package org.openjwc.client.net.llm

/** 供应商预设，用于设置页一键填充。 */
data class LlmPreset(
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val protocol: LlmProtocol = LlmProtocol.OPENAI,
)

object LlmPresets {
    val all: List<LlmPreset> = listOf(
        LlmPreset("openai", "OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
        LlmPreset("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
        LlmPreset("moonshot", "Moonshot / Kimi", "https://api.moonshot.cn/v1", "moonshot-v1-8k"),
        LlmPreset("zhipu", "智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash"),
        LlmPreset(
            "qwen", "阿里百炼 Qwen",
            "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus",
        ),
        LlmPreset("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini"),
        LlmPreset(
            "siliconflow", "SiliconFlow",
            "https://api.siliconflow.cn/v1", "Qwen/Qwen2.5-7B-Instruct",
        ),
        LlmPreset("groq", "Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
        LlmPreset("ollama", "Ollama（局域网/本地）", "http://192.168.1.2:11434/v1", "qwen2.5"),
        LlmPreset("custom", "自定义", "", ""),
    )

    fun byId(id: String): LlmPreset = all.firstOrNull { it.id == id } ?: all.last()

    fun matchId(baseUrl: String): String =
        all.firstOrNull { it.baseUrl.isNotBlank() && it.baseUrl == baseUrl }?.id ?: "custom"
}
