package org.openjwc.client.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.openjwc.client.net.llm.LlmProviderConfig

private val Context.llmStore by preferencesDataStore(name = "llm_prefs")

/** 保存 LLM provider 配置（不含 API Key）。 */
class LlmSettingsDataSource(private val context: Context) {

    private object Keys {
        val CONFIG = stringPreferencesKey("provider_config")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val config: Flow<LlmProviderConfig> = context.llmStore.data.map { prefs ->
        prefs[Keys.CONFIG]
            ?.let { raw -> runCatching { json.decodeFromString<LlmProviderConfig>(raw) }.getOrNull() }
            ?: LlmProviderConfig()
    }

    suspend fun current(): LlmProviderConfig = config.first()

    suspend fun save(config: LlmProviderConfig) {
        context.llmStore.edit { prefs ->
            prefs[Keys.CONFIG] = json.encodeToString(config)
        }
    }
}
