package org.openjwc.client.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.openjwc.client.data.dao.SettingsDao
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle
import java.util.UUID

class SettingsRepository(
    private val settingsDao: SettingsDao,
) {
    private val settingsMutex = Mutex()
    val userSettings: Flow<UserSettings?> = settingsDao.getSettings()

    suspend fun updateSettings(settings: UserSettings) {
        settingsDao.updateSettings(settings)
    }
    suspend fun getSettingsSnapshot(): UserSettings? {
        return settingsDao.getSettingsSnapshot()
    }

    // 懒加载
    suspend fun getOrGenerateDeviceId(): String {
        val current = settingsDao.getSettingsSnapshot() ?: UserSettings()

        if (current.uuidString.isBlank()) {
            val newId = UUID.randomUUID().toString()
            settingsDao.updateSettings(current.copy(uuidString = newId))
            return newId
        }
        return current.uuidString
    }

    private suspend fun updateSettingsInternal(transform: (UserSettings) -> UserSettings) {
        settingsMutex.withLock {
            val current = settingsDao.getSettingsSnapshot() ?: UserSettings(id = 0)
            val updated = transform(current)

            settingsDao.updateSettings(updated)
            Log.d("SettingsRepo", "Settings saved: $updated")
        }
    }

    suspend fun updateThemeColor(color: ColorType) = updateSettingsInternal {
        it.copy(themeColor = color)
    }

    suspend fun updateThemeStyle(style: DarkThemeStyle) = updateSettingsInternal {
        it.copy(themeStyle = style)
    }

    suspend fun updateHost(host: String) = updateSettingsInternal {
        it.copy(host = host)
    }
    suspend fun updatePort(port: Int)  = updateSettingsInternal {
        it.copy(port = port)
    }
    suspend fun updateUseHttp(useHttp: Boolean) = updateSettingsInternal {
        it.copy(useHttp = useHttp)
    }

    suspend fun updateAuthKey(key: String) = updateSettingsInternal {
        it.copy(authKey = key)
    }

    suspend fun updateFreshDays(days: Int) = updateSettingsInternal {
        it.copy(freshDays = days)
    }
}
