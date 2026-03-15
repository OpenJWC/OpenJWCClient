package org.openjwc.client.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.openjwc.client.data.dao.ChatDao
import org.openjwc.client.data.dao.SettingsDao
import org.openjwc.client.data.settings.UserSettings
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata

@Database(
    entities = [
        UserSettings::class,
        ChatMetadata::class,
        ChatMessage::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun chatDao(): ChatDao

    // 确保只有一个数据库实例
    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}