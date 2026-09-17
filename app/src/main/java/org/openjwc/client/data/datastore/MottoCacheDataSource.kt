package org.openjwc.client.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openjwc.client.data.models.Motto
import java.time.LocalDate

/** 缓存的一言（含抓取日期，用于「按天刷新」）。 */
@Serializable
data class CachedMotto(
    val text: String = Motto.DEFAULT_ONLINE.text,
    val author: String? = Motto.DEFAULT_ONLINE.author,
    val source: String? = null,
    val uuid: String? = null,
    /**
     * 抓取当天 `yyyy-MM-dd`。
     * 默认是「今天」：首次进入不联网，直接展示占位一言（乔鲁诺·乔巴纳），次日或手动刷新才请求。
     */
    val date: String = LocalDate.now().toString(),
) {
    fun toMotto(): Motto = Motto(
        text = text.ifBlank { Motto.DEFAULT_ONLINE.text },
        author = author,
        source = source,
        uuid = uuid,
        online = true,
    )

    val isFresh: Boolean get() = date == LocalDate.now().toString()

    companion object {
        fun from(motto: Motto) = CachedMotto(
            text = motto.text,
            author = motto.author,
            source = motto.source,
            uuid = motto.uuid,
            date = LocalDate.now().toString(),
        )
    }
}

private val Context.mottoStore by preferencesDataStore(name = "motto_cache")

/** 在线一言的本地缓存：避免频繁请求公益接口。 */
class MottoCacheDataSource(private val context: Context) {

    private object Keys {
        val MOTTO = stringPreferencesKey("motto")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val flow: Flow<CachedMotto> = context.mottoStore.data.map { prefs ->
        prefs[Keys.MOTTO]
            ?.let { raw -> runCatching { json.decodeFromString<CachedMotto>(raw) }.getOrNull() }
            ?: CachedMotto()
    }

    suspend fun current(): CachedMotto = flow.first()

    suspend fun save(cached: CachedMotto) {
        context.mottoStore.edit { prefs ->
            prefs[Keys.MOTTO] = json.encodeToString(cached)
        }
    }
}
