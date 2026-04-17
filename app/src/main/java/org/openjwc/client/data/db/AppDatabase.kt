package org.openjwc.client.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.openjwc.client.data.dao.ChatDao
import org.openjwc.client.data.dao.NewsDao
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.NoticeEntity

@Database(
    entities = [
        ChatMetadata::class,
        ChatMessage::class,
        NoticeEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun newsDao(): NewsDao

    companion object {
        private var INSTANCE: AppDatabase? = null
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS settings") // version 3 的设置已经放在了 dataStore 里
            }
        }
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS `favorite_notices` (
                `id` TEXT NOT NULL, 
                `label` TEXT NOT NULL, 
                `title` TEXT NOT NULL, 
                `date` TEXT NOT NULL, 
                `detailUrl` TEXT NOT NULL, 
                `isPage` INTEGER NOT NULL, 
                `contentText` TEXT, 
                `attachmentUrls` TEXT, 
            PRIMARY KEY(`id`)
            )
        )
        """.trimIndent())
            }
        }

        // TODO: val MIGRATION_3_4
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}