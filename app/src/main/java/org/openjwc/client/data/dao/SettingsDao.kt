package org.openjwc.client.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.openjwc.client.data.settings.UserSettings

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: UserSettings)

    // id = 0 是因为整个应用只会有一个 settings
    @Query("SELECT * FROM settings WHERE id = 0")
    fun getSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM settings WHERE id = 0 LIMIT 1")
    suspend fun getSettingsSnapshot(): UserSettings?
}