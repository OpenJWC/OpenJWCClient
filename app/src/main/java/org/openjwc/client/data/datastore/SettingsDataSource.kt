package org.openjwc.client.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.openjwc.client.net.models.Proxy
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle
import org.openjwc.client.ui.theme.toColorType
import org.openjwc.client.ui.theme.toStorageString

data class UserSettings(
    val policyAgreed: Boolean = false,
    val themeStyle: DarkThemeStyle = DarkThemeStyle.Auto,
    val themeColor: ColorType = ColorType.Dynamic,
    val host: String = "101.132.106.186",
    val port: Int = 8001,
    val useHttp: Boolean = false,
    val freshDays: Int = 21,
    val backgroundPath: String? = null,
    val backgroundAlpha: Float = 0.3f,
    val proxy: Proxy = Proxy.NoProxy(),
    val languageCode: String? = null,
)

private val Context.settingsStore by preferencesDataStore(name = "user_settings")
class SettingsDataSource(private val context: Context) {

    object Keys {
        val POLICY_AGREED = booleanPreferencesKey("policy_agreed")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val HOST = stringPreferencesKey("host")
        val PORT = intPreferencesKey("port")
        val USE_HTTP = booleanPreferencesKey("use_http")
        val FRESH_DAYS = intPreferencesKey("fresh_days")
        val BACKGROUND_PATH = stringPreferencesKey("background_path")
        val BACKGROUND_ALPHA = floatPreferencesKey("background_alpha")
        val PROXY_TYPE = stringPreferencesKey("proxy_type")
        val PROXY_ADDRESS = stringPreferencesKey("proxy_address")
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
    }

    val userSettings: Flow<UserSettings> = context.settingsStore.data.map { prefs ->
        val default = UserSettings()
        default.copy(
            policyAgreed = prefs[Keys.POLICY_AGREED] ?: default.policyAgreed,
            themeColor = prefs[Keys.THEME_COLOR]?.toColorType() ?: default.themeColor,
            themeStyle = prefs[Keys.THEME_STYLE]?.let {
                runCatching { DarkThemeStyle.valueOf(it) }.getOrDefault(default.themeStyle)
            } ?: default.themeStyle,

            host = prefs[Keys.HOST] ?: default.host,
            port = prefs[Keys.PORT] ?: default.port,
            useHttp = prefs[Keys.USE_HTTP] ?: default.useHttp,
            freshDays = prefs[Keys.FRESH_DAYS] ?: default.freshDays,
            backgroundPath = prefs[Keys.BACKGROUND_PATH]?.takeIf { it.isNotBlank() }
                ?: default.backgroundPath,
            backgroundAlpha = prefs[Keys.BACKGROUND_ALPHA] ?: default.backgroundAlpha,

            proxy = when (prefs[Keys.PROXY_TYPE]) {
                "http" -> Proxy.HttpProxy(
                    host = prefs[Keys.PROXY_ADDRESS] ?: "localhost",
                    port = prefs[Keys.PROXY_PORT] ?: 8080
                )

                "socks" -> Proxy.SocksProxy(
                    host = prefs[Keys.PROXY_ADDRESS] ?: "localhost",
                    port = prefs[Keys.PROXY_PORT] ?: 8080
                )

                else -> Proxy.NoProxy()
            },
            languageCode = prefs[Keys.LANGUAGE_CODE]?.takeIf { it.isNotBlank() }
                ?: default.languageCode,
        )
    }
    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        context.settingsStore.edit { it[key] = value }
    }

    suspend fun saveColorType(color: ColorType) {
        context.settingsStore.edit { it[Keys.THEME_COLOR] = color.toStorageString() }
    }

    suspend fun saveBackgroundPath(path: String?) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.BACKGROUND_PATH] = path ?: ""
        }
    }

    suspend fun saveProxy(proxy: Proxy) {
        context.settingsStore.edit { prefs ->
            when (proxy) {
                is Proxy.NoProxy -> {
                    prefs[Keys.PROXY_TYPE] = ""
                }

                is Proxy.HttpProxy -> {
                    prefs[Keys.PROXY_TYPE] = "http"
                    prefs[Keys.PROXY_ADDRESS] = proxy.host
                    prefs[Keys.PROXY_PORT] = proxy.port
                }

                is Proxy.SocksProxy -> {
                    prefs[Keys.PROXY_TYPE] = "socks"
                    prefs[Keys.PROXY_ADDRESS] = proxy.host
                    prefs[Keys.PROXY_PORT] = proxy.port
                }
            }
        }
    }

    suspend fun saveLanguageCode(code: String?) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = code ?: ""
        }
    }
}
