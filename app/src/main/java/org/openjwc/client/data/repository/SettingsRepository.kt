package org.openjwc.client.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.openjwc.client.data.dao.SettingsDao
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle

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
}
