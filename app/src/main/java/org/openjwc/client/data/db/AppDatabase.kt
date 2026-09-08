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
import org.openjwc.client.data.models.NewsCacheEntity
import org.openjwc.client.data.models.NewsLabelCacheEntity
import org.openjwc.client.data.models.NoticeEntity
import org.openjwc.client.data.models.TableMetadata

@Database(
    entities = [
        ChatMetadata::class,
        ChatMessage::class,
        NoticeEntity::class,
        NewsCacheEntity::class,
        NewsLabelCacheEntity::class,
        Course::class,
        TableMetadata::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun newsDao(): NewsDao
    abstract fun courseDao(): CourseDao
    abstract fun tableDao(): TableDao

    companion object {
        @Volatile
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

        // 5 → 6：新增资讯缓存表 + 标签缓存表；重建 favorite_notices 增加数据源维度
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `news_cache` (
                        `host` TEXT NOT NULL,
                        `port` INTEGER NOT NULL,
                        `noticeId` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `sortTime` INTEGER NOT NULL,
                        `detailUrl` TEXT NOT NULL,
                        `isPage` INTEGER NOT NULL,
                        `contentText` TEXT,
                        `attachmentUrls` TEXT,
                        `cachedAt` INTEGER NOT NULL,
                        `notified` INTEGER NOT NULL,
                        PRIMARY KEY(`host`, `port`, `noticeId`)
                    )
                """.trimIndent())

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_news_cache_host_port_label` " +
                        "ON `news_cache` (`host`, `port`, `label`)"
                )

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `news_cache_labels` (
                        `host` TEXT NOT NULL,
                        `port` INTEGER NOT NULL,
                        `labels` TEXT NOT NULL,
                        `cachedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`host`, `port`)
                    )
                """.trimIndent())

                // SQLite 无法修改主键，重建表：收藏行归入 host='' 遗留分区，待启动时归属当前源
                db.execSQL("DROP TABLE IF EXISTS `favorite_notices_new`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `favorite_notices_new` (
                        `host` TEXT NOT NULL,
                        `port` INTEGER NOT NULL,
                        `id` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `detailUrl` TEXT NOT NULL,
                        `isPage` INTEGER NOT NULL,
                        `contentText` TEXT,
                        `attachmentUrls` TEXT,
                        PRIMARY KEY(`host`, `port`, `id`)
                    )
                """.trimIndent())

                db.execSQL(
                    "INSERT INTO `favorite_notices_new` " +
                        "(`host`, `port`, `id`, `label`, `title`, `date`, `detailUrl`, `isPage`, `contentText`, `attachmentUrls`) " +
                        "SELECT '', 0, `id`, `label`, `title`, `date`, `detailUrl`, `isPage`, `contentText`, `attachmentUrls` " +
                        "FROM `favorite_notices`"
                )
                db.execSQL("DROP TABLE `favorite_notices`")
                db.execSQL("ALTER TABLE `favorite_notices_new` RENAME TO `favorite_notices`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6) // 3. 添加新迁移脚本
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}