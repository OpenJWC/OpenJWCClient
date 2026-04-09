package org.openjwc.client.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.openjwc.client.data.settings.SettingsDataSource
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.log.Logger
import org.openjwc.client.net.hitokoto.fetchHitokoto
import org.openjwc.client.net.models.Hitokoto
import org.openjwc.client.net.models.NetClient
import org.openjwc.client.net.models.NetworkResult
import org.openjwc.client.net.models.Proxy
import org.openjwc.client.net.models.SuccessResponse
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID

class SettingsRepository(
    private val dataSource: SettingsDataSource,
    private val context: Context
) {
    private val label = "SettingsRepository"
    val userSettings: Flow<UserSettings> = dataSource.userSettings
    val keys = SettingsDataSource.Keys

    suspend fun getSettingsSnapshot(): UserSettings {
        return userSettings.first()
    }

    suspend fun getOrGenerateDeviceId(): String {
        val current = getSettingsSnapshot()
        if (current.uuidString.isBlank()) {
            val newId = UUID.randomUUID().toString()
            updateUUID(newId)
            return newId
        }
        return current.uuidString
    }

    suspend fun tryRefreshHitokoto(): NetworkResult<SuccessResponse<Hitokoto>> {
        try {
            val settings = getSettingsSnapshot()
            val apiService =
                NetClient.getService(
                    settings.host,
                    settings.port,
                    settings.useHttp,
                    settings.proxy
                )

            val result = apiService.fetchHitokoto(
                settings.authKey,
                settings.uuidString,
            )
            if (result is NetworkResult.Success) {
                Logger.i(label, "Refresh hitokoto: ${LocalDate.now()}")
                dataSource.saveHitokoto(result.response.data)
            }
            return result
        } catch (e: Exception) {
            Logger.e(label, "Failed to refresh hitokoto: ${e.localizedMessage}")
            return NetworkResult.Error("Unknown Error")
        }
    }

    suspend fun agreePolicy() = dataSource.save(keys.POLICY_AGREED, true)

    suspend fun updateThemeColor(color: ColorType) = dataSource.saveColorType(color)

    suspend fun updateThemeStyle(style: DarkThemeStyle) =
        dataSource.save(keys.THEME_STYLE, style.name)

    suspend fun updateHost(host: String) = dataSource.save(keys.HOST, host)

    suspend fun updatePort(port: Int) = dataSource.save(keys.PORT, port)

    suspend fun updateUseHttp(useHttp: Boolean) = dataSource.save(keys.USE_HTTP, useHttp)

    suspend fun updateAuthKey(key: String) = dataSource.save(keys.AUTH_KEY, key)

    suspend fun updateFreshDays(days: Int) = dataSource.save(keys.FRESH_DAYS, days)

    private suspend fun updateBackgroundPath(path: String?) = dataSource.saveBackgroundPath(path)
    private suspend fun updateUUID(uuid: String) = dataSource.save(keys.UUID_STRING, uuid)

    suspend fun updateBackgroundAlpha(alpha: Float) = dataSource.save(keys.BACKGROUND_ALPHA, alpha)

    suspend fun updateBackground(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val bgDir = File(context.filesDir, "backgrounds").apply {
                if (!exists()) mkdirs()
            }

            bgDir.listFiles()?.forEach { it.delete() }

            val newFileName = "bg_${System.currentTimeMillis()}.jpg"
            val targetFile = File(bgDir, newFileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            updateBackgroundPath(targetFile.absolutePath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteBackground(): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentPath = getSettingsSnapshot().backgroundPath

            if (!currentPath.isNullOrBlank()) {
                val file = File(currentPath)
                if (file.exists()) {
                    file.delete()
                }
            }
            updateBackgroundPath(null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateProxy(proxy: Proxy) = dataSource.saveProxy(proxy)
}