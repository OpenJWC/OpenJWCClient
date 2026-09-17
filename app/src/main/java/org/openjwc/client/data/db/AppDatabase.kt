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
import org.openjwc.client.data.dao.DailyReportDao
import org.openjwc.client.data.dao.NoticeDao
import org.openjwc.client.data.dao.SourceDao
import org.openjwc.client.data.dao.TableDao
import org.openjwc.client.data.models.ChatMessage
import org.openjwc.client.data.models.ChatMetadata
import org.openjwc.client.data.models.ChatToolCall
import org.openjwc.client.data.models.Course
import org.openjwc.client.data.models.DailyReportEntity
import org.openjwc.client.data.models.NoticeEntity
import org.openjwc.client.data.models.SourceEntity
import org.openjwc.client.data.models.TableMetadata

@Database(
    entities = [
        ChatMetadata::class,
        ChatMessage::class,
        ChatToolCall::class,
        NoticeEntity::class,
        DailyReportEntity::class,
        Course::class,
        TableMetadata::class,
        SourceEntity::class
    ],
    version = 14,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun noticeDao(): NoticeDao
    abstract fun dailyReportDao(): DailyReportDao
    abstract fun courseDao(): CourseDao
    abstract fun tableDao(): TableDao
    abstract fun sourceDao(): SourceDao

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

        // 6 → 7：聊天消息新增附件 notice id 列表，用于发送历史上下文
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `chat_messages` ADD COLUMN `attachmentIds` TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        // 7 → 8：新增资讯数据源（脚本）表
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notice_sources` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `origin` TEXT NOT NULL,
                        `scriptFile` TEXT,
                        `domains` TEXT NOT NULL,
                        `labels` TEXT NOT NULL,
                        `scheduleMinutes` INTEGER NOT NULL,
                        `subscribed` INTEGER NOT NULL,
                        `lastRunAt` INTEGER,
                        `lastCount` INTEGER NOT NULL,
                        `lastError` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        // 8 → 9：资讯改为本地脚本数据源，移除服务端标签缓存表
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `news_cache_labels`")
            }
        }

        // 9 → 10：资讯缓存 + 收藏合并为统一语料表 `notices`，并新增日报表
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notices` (
                        `id` TEXT NOT NULL,
                        `sourceId` TEXT,
                        `label` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `publishedAt` INTEGER NOT NULL,
                        `publishedDay` TEXT NOT NULL,
                        `detailUrl` TEXT NOT NULL,
                        `isPage` INTEGER NOT NULL,
                        `content` TEXT,
                        `attachments` TEXT,
                        `fetchedAt` INTEGER NOT NULL,
                        `notified` INTEGER NOT NULL,
                        `favorite` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notices_publishedAt` ON `notices` (`publishedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notices_publishedDay` ON `notices` (`publishedDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notices_label_publishedAt` ON `notices` (`label`, `publishedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notices_sourceId` ON `notices` (`sourceId`)")

                // 旧缓存按 `source:<id>` 分区，迁移时还原 sourceId
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `notices`
                        (`id`, `sourceId`, `label`, `title`, `publishedAt`, `publishedDay`,
                         `detailUrl`, `isPage`, `content`, `attachments`, `fetchedAt`, `notified`, `favorite`)
                    SELECT `noticeId`,
                           CASE WHEN `host` LIKE 'source:%' THEN substr(`host`, 8) ELSE NULL END,
                           `label`, `title`, `sortTime`, substr(`date`, 1, 10),
                           `detailUrl`, `isPage`, `contentText`, `attachmentUrls`, `cachedAt`, `notified`, 0
                    FROM `news_cache`
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `notices`
                        (`id`, `sourceId`, `label`, `title`, `publishedAt`, `publishedDay`,
                         `detailUrl`, `isPage`, `content`, `attachments`, `fetchedAt`, `notified`, `favorite`)
                    SELECT `id`, NULL, `label`, `title`, 0, substr(`date`, 1, 10),
                           `detailUrl`, `isPage`, `contentText`, `attachmentUrls`, 0, 0, 1
                    FROM `favorite_notices`
                    """.trimIndent()
                )
                db.execSQL("UPDATE `notices` SET `favorite` = 1 WHERE `id` IN (SELECT `id` FROM `favorite_notices`)")

                db.execSQL("DROP TABLE IF EXISTS `news_cache`")
                db.execSQL("DROP TABLE IF EXISTS `favorite_notices`")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `daily_reports` (
                        `day` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `sourceCount` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`day`)
                    )
                """.trimIndent())
            }
        }

        // 10 → 11：聊天消息补充运行状态/交付方式/失败 code，并新增工具轨迹表
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'COMPLETED'")
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `runId` TEXT")
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `delivery` TEXT")
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `errorCode` TEXT")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chat_tool_calls` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `messageId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `code` TEXT,
                        `durationMs` INTEGER,
                        FOREIGN KEY(`messageId`) REFERENCES `chat_messages`(`messageId`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_tool_calls_messageId` ON `chat_tool_calls` (`messageId`)")
            }
        }

        // 11 → 12：日报记录失败原因，便于界面展示与手动重试
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_reports` ADD COLUMN `error` TEXT")
            }
        }

        // 12 → 13：资讯正文记录提取格式版本，格式升级后旧条目会自动重抓补正文
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `notices` ADD COLUMN `contentVersion` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // 13 → 14：工具调用记录指向的本地对象 id（read_notice 的资讯 id，供点击进详情）
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_tool_calls` ADD COLUMN `targetId` TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(
                        MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                        MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14
                    )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
