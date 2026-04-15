package org.openjwc.client.data.datastore

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.openjwc.client.log.Logger
import java.util.UUID

data class AuthSession(
    val username: String? = null,
    val email: String? = null,
    val uuid: String = UUID.randomUUID().toString(),
    val deviceName: String = Build.MODEL,
    val token: String? = null,
    val isLoggedIn: Boolean = false
)
private val Context.authStore by preferencesDataStore(name = "auth_prefs")
class AuthDataSource(private val context: Context) {
    object Keys {
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val UUID_STRING = stringPreferencesKey("uuid_string")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val TOKEN = stringPreferencesKey("token")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    val authSession: Flow<AuthSession> = context.authStore.data.map { prefs ->
        AuthSession(
            username = prefs[Keys.USERNAME].takeIf { it?.isNotBlank() == true },
            email = prefs[Keys.EMAIL].takeIf { it?.isNotBlank() == true },
            uuid = prefs[Keys.UUID_STRING].takeIf { it?.isNotBlank() == true } ?: UUID.randomUUID().toString(),
            deviceName = prefs[Keys.DEVICE_NAME].takeIf { it?.isNotBlank() == true } ?: Build.MODEL,
            token = prefs[Keys.TOKEN].takeIf { it?.isNotBlank() == true },
            isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false
        )
    }

    suspend fun saveSession(username: String, email: String, token: String) {
        context.authStore.edit { prefs ->
            prefs[Keys.USERNAME] = username
            prefs[Keys.EMAIL] = email
            prefs[Keys.TOKEN] = token
            prefs[Keys.IS_LOGGED_IN] = true
        }
        Logger.d("AuthDataSource", "saveSession: $username, $email, $token")
    }

    suspend fun clearSession() {
        context.authStore.edit { prefs ->
            prefs[Keys.USERNAME] = ""
            prefs[Keys.TOKEN] = ""
            prefs[Keys.EMAIL] = ""
            prefs[Keys.IS_LOGGED_IN] = false
        }
    }

    suspend fun getOrCreateUuid(): String {
        return context.authStore.edit { prefs ->
            if (prefs[Keys.UUID_STRING] == null) {
                prefs[Keys.UUID_STRING] = UUID.randomUUID().toString()
            }
        }.let { it[Keys.UUID_STRING] ?: "" }
    }

    suspend fun getOrCreateDeviceName(): String {
        return context.authStore.edit { prefs ->
            if (prefs[Keys.DEVICE_NAME] == null) {
                prefs[Keys.DEVICE_NAME] = "${Build.MANUFACTURER} ${Build.MODEL}"
            }
        }.let { it[Keys.DEVICE_NAME] ?: "" }
    }
}