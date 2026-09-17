package org.openjwc.client.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.openjwc.client.data.datastore.SettingsDataSource
import org.openjwc.client.data.datastore.UserSettings
import org.openjwc.client.log.Logger
import org.openjwc.client.net.models.Proxy
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle
import java.io.File
import java.io.FileOutputStream

class SettingsRepository(
    private val settingsDataSource: SettingsDataSource,
    private val context: Context
) {
    private val label = "SettingsRepository"
    val userSettings: Flow<UserSettings> = settingsDataSource.userSettings
    val keys = SettingsDataSource.Keys

    suspend fun getSettingsSnapshot(): UserSettings {
        return userSettings.first()
    }

    suspend fun agreePolicy() = settingsDataSource.save(keys.POLICY_AGREED, true)

    suspend fun updateThemeColor(color: ColorType) = settingsDataSource.saveColorType(color)

    suspend fun updateThemeStyle(style: DarkThemeStyle) =
        settingsDataSource.save(keys.THEME_STYLE, style.name)

    suspend fun updateFreshDays(days: Int) = settingsDataSource.save(keys.FRESH_DAYS, days)

    suspend fun updateCrawlDaysGap(days: Int) = settingsDataSource.save(keys.CRAWL_DAYS_GAP, days)

    suspend fun updateMotto(text: String, author: String) {
        settingsDataSource.save(keys.MOTTO_TEXT, text)
        settingsDataSource.save(keys.MOTTO_AUTHOR, author)
    }

    suspend fun updateMottoOnline(enabled: Boolean) =
        settingsDataSource.save(keys.MOTTO_ONLINE, enabled)

    suspend fun updateHitokotoCategory(code: String) =
        settingsDataSource.save(keys.HITOKOTO_CATEGORY, code)

    suspend fun updateHitokotoMaxLength(length: Int) =
        settingsDataSource.save(keys.HITOKOTO_MAX_LENGTH, length)

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
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Logger.e(label, "Failed to open input stream for URI: $uri")
                return@withContext false
            }
            inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            updateBackgroundPath(targetFile.absolutePath)
            true
        } catch (e: Exception) {
            Logger.e(label, "Failed to update background: ${e.localizedMessage}")
            false
        }
    }

    suspend fun deleteBackground(): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentPath = getSettingsSnapshot().backgroundPath

            if (!currentPath.isNullOrBlank()) {
                val file = File(currentPath)
                if (file.exists() && !file.delete()) {
                    Logger.w(label, "Failed to delete background file: $currentPath")
                }
            }
            updateBackgroundPath(null)
            true
        } catch (e: Exception) {
            Logger.e(label, "Failed to delete background: ${e.localizedMessage}")
            false
        }
    }

    suspend fun updateProxy(proxy: Proxy) = settingsDataSource.saveProxy(proxy)

    suspend fun updateLanguageCode(code: String?) = settingsDataSource.saveLanguageCode(code)

    suspend fun updateShowTimeline(show: Boolean) = settingsDataSource.save(keys.SHOW_TIMELINE, show)
    suspend fun updateShowDate(show: Boolean) = settingsDataSource.save(keys.SHOW_DATE, show)
    suspend fun updateShowPeriodTime(show: Boolean) = settingsDataSource.save(keys.SHOW_PERIOD_TIME, show)
    suspend fun updateShowNonCurrentWeek(show: Boolean) = settingsDataSource.save(keys.SHOW_NON_CURRENT_WEEK, show)

    suspend fun updateNewsNotificationEnabled(enabled: Boolean) =
        settingsDataSource.save(keys.NEWS_NOTIFICATION_ENABLED, enabled)

    suspend fun updateNewsCheckIntervalMinutes(minutes: Int) =
        settingsDataSource.save(keys.NEWS_CHECK_INTERVAL_MINUTES, minutes)

    suspend fun updateCourseReminderEnabled(enabled: Boolean) =
        settingsDataSource.save(keys.COURSE_REMINDER_ENABLED, enabled)

    suspend fun updatePermissionReminderDismissed(dismissed: Boolean) =
        settingsDataSource.save(keys.PERMISSION_REMINDER_DISMISSED, dismissed)

    suspend fun updateAutoStartEnabled(enabled: Boolean) =
        settingsDataSource.save(keys.AUTO_START_ENABLED, enabled)

    suspend fun updateDailyReportEnabled(enabled: Boolean) =
        settingsDataSource.save(keys.DAILY_REPORT_ENABLED, enabled)

    suspend fun updateDailyReportTime(time: String) =
        settingsDataSource.save(keys.DAILY_REPORT_TIME, time)

    suspend fun getToggleState(id: String): Boolean = settingsDataSource.getToggleState(id)

    suspend fun saveToggleState(id: String, value: Boolean) = settingsDataSource.saveToggleState(id, value)
}
