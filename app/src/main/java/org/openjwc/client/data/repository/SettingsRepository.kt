package org.openjwc.client.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.openjwc.client.data.datastore.AuthDataSource
import org.openjwc.client.data.datastore.CachedDataSource
import org.openjwc.client.data.datastore.CachedHitokoto
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.datastore.UserSettings
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

class SettingsRepository(
    private val settingsDataSource: SettingsDataSource,
    private val cachedDataSource: CachedDataSource,
    private val authDataSource: AuthDataSource,
    private val context: Context
) {
    private val label = "SettingsRepository"
    val userSettings: Flow<UserSettings> = settingsDataSource.userSettings
    val hitokotoFlow: Flow<CachedHitokoto> = cachedDataSource.cachedHitokotoFlow
    val keys = SettingsDataSource.Keys

    suspend fun getSettingsSnapshot(): UserSettings {
        return userSettings.first()
    }

    suspend fun tryRefreshHitokoto(): NetworkResult<SuccessResponse<Hitokoto>> {
        if (!authDataSource.authSession.first().isLoggedIn) return NetworkResult.Error("Not logged in")
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
                authDataSource.authSession.first().token ?: throw Exception("No token"),
                authDataSource.authSession.first().uuid,
            )
            if (result is NetworkResult.Success) {
                Logger.i(label, "Refresh hitokoto: ${LocalDate.now()}")
                cachedDataSource.saveHitokoto(result.response.data)
            }
            return result
        } catch (e: Exception) {
            Logger.e(label, "Failed to refresh hitokoto: ${e.localizedMessage}")
            return NetworkResult.Error("Unknown Error")
        }
    }

    suspend fun agreePolicy() = settingsDataSource.save(keys.POLICY_AGREED, true)

    suspend fun updateThemeColor(color: ColorType) = settingsDataSource.saveColorType(color)

    suspend fun updateThemeStyle(style: DarkThemeStyle) =
        settingsDataSource.save(keys.THEME_STYLE, style.name)

    suspend fun updateHost(host: String) = settingsDataSource.save(keys.HOST, host)

    suspend fun updatePort(port: Int) = settingsDataSource.save(keys.PORT, port)

    suspend fun updateUseHttp(useHttp: Boolean) = settingsDataSource.save(keys.USE_HTTP, useHttp)

    suspend fun updateFreshDays(days: Int) = settingsDataSource.save(keys.FRESH_DAYS, days)

    private suspend fun updateBackgroundPath(path: String?) =
        settingsDataSource.saveBackgroundPath(path)

    suspend fun updateBackgroundAlpha(alpha: Float) =
        settingsDataSource.save(keys.BACKGROUND_ALPHA, alpha)

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

    suspend fun updateProxy(proxy: Proxy) = settingsDataSource.saveProxy(proxy)
}