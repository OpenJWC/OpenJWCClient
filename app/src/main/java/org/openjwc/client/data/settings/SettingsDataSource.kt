package org.openjwc.client.data.settings

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle
import org.openjwc.client.ui.theme.toColorType
import org.openjwc.client.ui.theme.toStorageString

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class SettingsDataSource(private val context: Context) {
    object Keys {
        val POLICY_AGREED = booleanPreferencesKey("policy_agreed")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val HOST = stringPreferencesKey("host")
        val PORT = intPreferencesKey("port")
        val USE_HTTP = booleanPreferencesKey("use_http")
        val AUTH_KEY = stringPreferencesKey("auth_key")
        val FRESH_DAYS = intPreferencesKey("fresh_days")
        val UUID_STRING = stringPreferencesKey("uuid_string")
        val BACKGROUND_PATH = stringPreferencesKey("background_path")
        val BACKGROUND_ALPHA = floatPreferencesKey("background_alpha")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val default = UserSettings()

        default.copy(
            policyAgreed = prefs[Keys.POLICY_AGREED] ?: default.policyAgreed,
            themeColor = prefs[Keys.THEME_COLOR].toColorType(),
            themeStyle = prefs[Keys.THEME_STYLE]?.let {
                runCatching { DarkThemeStyle.valueOf(it) }.getOrDefault(default.themeStyle)
            } ?: default.themeStyle,

            host = prefs[Keys.HOST] ?: default.host,
            port = prefs[Keys.PORT] ?: default.port,
            useHttp = prefs[Keys.USE_HTTP] ?: default.useHttp,
            authKey = prefs[Keys.AUTH_KEY] ?: default.authKey,
            freshDays = prefs[Keys.FRESH_DAYS] ?: default.freshDays,
            uuidString = prefs[Keys.UUID_STRING] ?: default.uuidString,
            backgroundPath = prefs[Keys.BACKGROUND_PATH]?.takeIf { it.isNotBlank() } ?: default.backgroundPath,
            backgroundAlpha = prefs[Keys.BACKGROUND_ALPHA] ?: default.backgroundAlpha
        )
    }

    suspend fun updateSettings(transform: (MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs ->
            transform(prefs)
        }
    }

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun saveColorType(color: ColorType) {
        context.dataStore.edit { it[Keys.THEME_COLOR] = color.toStorageString() }
    }

    suspend fun saveBackgroundPath(path: String?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_PATH] = path ?: ""
        }
    }

}