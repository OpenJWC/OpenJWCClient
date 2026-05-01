package org.openjwc.client.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.openjwc.client.data.dao.ChatDao
import org.openjwc.client.data.dao.CourseDao
import org.openjwc.client.data.dao.NewsDao
import org.openjwc.client.data.dao.TableDao
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.NoticeEntity
import org.openjwc.client.data.models.TableMetadata

@Database(
    entities = [
        ChatMetadata::class,
        ChatMessage::class,
        NoticeEntity::class,
        Course::class,
        TableMetadata::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun newsDao(): NewsDao
    abstract fun courseDao(): CourseDao
    abstract fun tableDao(): TableDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS settings")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
                """.trimIndent())
            }
        }

        // 2. 定义从 4 到 5 的迁移脚本
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建课表元数据表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `table_metadata` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `tableName` TEXT NOT NULL, 
                        `semesterConfig` TEXT NOT NULL, 
                        `isCurrent` INTEGER NOT NULL
                    )
                """.trimIndent())

                // 创建课程表，并设置外键和索引
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `courses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `tableId` INTEGER NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `teacher` TEXT NOT NULL, 
                        `location` TEXT NOT NULL, 
                        `dayOfWeek` INTEGER NOT NULL, 
                        `startPeriod` INTEGER NOT NULL, 
                        `duration` INTEGER NOT NULL, 
                        `color` INTEGER NOT NULL, 
                        `weekRule` TEXT NOT NULL, 
                        `note` TEXT NOT NULL, 
                        FOREIGN KEY(`tableId`) REFERENCES `table_metadata`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_courses_tableId` ON `courses` (`tableId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5) // 3. 添加新迁移脚本
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}