package org.openjwc.client.data.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.openjwc.client.log.Logger

/**
 * 用户 API Key 的安全存储。
 * 用 Android Keystore 生成的 MasterKey 加密 SharedPreferences，
 * 明文 Key 不落盘、不进日志、不进 APK。
 */
class LlmKeyStore(context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.onFailure {
        Logger.e(TAG, "初始化加密存储失败: ${it.message}", it)
    }.getOrNull()

    fun get(providerId: String): String? =
        prefs?.getString(key(providerId), null)?.takeIf { it.isNotBlank() }

    fun save(providerId: String, apiKey: String) {
        prefs?.edit()?.putString(key(providerId), apiKey)?.apply()
    }

    fun clear(providerId: String) {
        prefs?.edit()?.remove(key(providerId))?.apply()
    }

    private fun key(providerId: String) = "api_key_$providerId"

    private companion object {
        const val TAG = "LlmKeyStore"
        const val FILE_NAME = "llm_keys"
    }
}
