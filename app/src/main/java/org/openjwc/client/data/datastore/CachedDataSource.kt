package org.openjwc.client.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.openjwc.client.net.models.Hitokoto
import java.time.LocalDate


data class CachedHitokoto(
    val text: String = Hitokoto().text,
    val author: String? = Hitokoto().author,
    val date: LocalDate = LocalDate.now()
)
private val Context.cacheStore by preferencesDataStore(name = "cached_prefs")
class CachedDataSource(private val context: Context) {
    object Keys {
        val HITOKOTO_JSON = stringPreferencesKey("hitokoto_json")
    }

    suspend fun saveHitokoto(hitokoto: Hitokoto) {
        val data = CachedHitokoto(
            text = hitokoto.text,
            author = hitokoto.author,
            date = LocalDate.now()
        )
        context.cacheStore.edit {
            it[Keys.HITOKOTO_JSON] = Json.encodeToString(data)
        }
    }

    // 读取
    val cachedHitokotoFlow: Flow<CachedHitokoto> = context.cacheStore.data.map { prefs ->
        val json = prefs[Keys.HITOKOTO_JSON]
        if (json == null) {
            CachedHitokoto()
        } else {
            Json.decodeFromString<CachedHitokoto>(json)
        }
    }
}