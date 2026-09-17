package org.openjwc.client.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.openjwc.client.net.models.Proxy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle
import org.openjwc.client.ui.theme.toColorType
import org.openjwc.client.ui.theme.toStorageString

data class UserSettings(
    val policyAgreed: Boolean = false,
    val themeStyle: DarkThemeStyle = DarkThemeStyle.Auto,
    val themeColor: ColorType = ColorType.Dynamic,
    val freshDays: Int = 21,
    val backgroundPath: String? = null,
    val backgroundAlpha: Float = 0.3f,
    /** 网络代理（目前仅「检查更新」使用）。 */
    val proxy: Proxy = Proxy.NoProxy(),
    val languageCode: String? = null,
    val currentTableId: Long? = null,
    val showTimeline: Boolean = true,
    val showDate: Boolean = true,
    val showPeriodTime: Boolean = true,
    val showNonCurrentWeek: Boolean = true,
    val newsNotificationEnabled: Boolean = false,
    val newsCheckIntervalMinutes: Int = 60,
    val courseReminderEnabled: Boolean = false,
    val permissionReminderDismissed: Boolean = false,
    val autoStartEnabled: Boolean = false,
    val dailyReportEnabled: Boolean = false,
    /** `HH:mm`，本地时区。 */
    val dailyReportTime: String = "00:10",
    /** 抓取回溯天数：脚本只抓最近这么多天的资讯（与显示用的 freshDays 无关）。 */
    val crawlDaysGap: Int = 200,
    /** 每日一言（对齐后端 `motto_text` 默认值）。 */
    val mottoText: String = "笃学尚行",
    /** 作者；留空或填「佚名」时按无作者处理。 */
    val mottoAuthor: String = "",
    /** 是否使用在线一言（hitokoto.cn）；默认开启。 */
    val mottoOnline: Boolean = true,
    /** 在线一言分类字母（空表示不限）。 */
    val hitokotoCategory: String = "",
    /** 在线一言的最大长度（接口 `max_length`）。 */
    val hitokotoMaxLength: Int = 30
)

private val Context.settingsStore by preferencesDataStore(name = "user_settings")
class SettingsDataSource(private val context: Context) {

    object Keys {
        val POLICY_AGREED = booleanPreferencesKey("policy_agreed")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val FRESH_DAYS = intPreferencesKey("fresh_days")
        val BACKGROUND_PATH = stringPreferencesKey("background_path")
        val BACKGROUND_ALPHA = floatPreferencesKey("background_alpha")
        val PROXY_TYPE = stringPreferencesKey("proxy_type")
        val PROXY_ADDRESS = stringPreferencesKey("proxy_address")
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val CURRENT_TABLE_ID = longPreferencesKey("current_table_id")
        val SHOW_TIMELINE = booleanPreferencesKey("show_timeline")
        val SHOW_DATE = booleanPreferencesKey("show_date")
        val SHOW_PERIOD_TIME = booleanPreferencesKey("show_period_time")
        val SHOW_NON_CURRENT_WEEK = booleanPreferencesKey("show_non_current_week")
        val NEWS_NOTIFICATION_ENABLED = booleanPreferencesKey("news_notification_enabled")
        val NEWS_CHECK_INTERVAL_MINUTES = intPreferencesKey("news_check_interval_minutes")
        val COURSE_REMINDER_ENABLED = booleanPreferencesKey("course_reminder_enabled")
        val PERMISSION_REMINDER_DISMISSED = booleanPreferencesKey("permission_reminder_dismissed")
        val AUTO_START_ENABLED = booleanPreferencesKey("auto_start_enabled")
        val DAILY_REPORT_ENABLED = booleanPreferencesKey("daily_report_enabled")
        val DAILY_REPORT_TIME = stringPreferencesKey("daily_report_time")
        /** 用户删除过的内置数据源 id：避免 syncBuiltIns 把它们重新插回来。 */
        val DELETED_SOURCE_IDS = stringSetPreferencesKey("deleted_source_ids")
        val CRAWL_DAYS_GAP = intPreferencesKey("crawl_days_gap")
        val MOTTO_TEXT = stringPreferencesKey("motto_text")
        val MOTTO_AUTHOR = stringPreferencesKey("motto_author")
        val MOTTO_ONLINE = booleanPreferencesKey("motto_online")
        val HITOKOTO_CATEGORY = stringPreferencesKey("hitokoto_category")
        val HITOKOTO_MAX_LENGTH = intPreferencesKey("hitokoto_max_length")
    }

    val userSettings: Flow<UserSettings> = context.settingsStore.data.map { prefs ->
        val default = UserSettings()
        default.copy(
            policyAgreed = prefs[Keys.POLICY_AGREED] ?: default.policyAgreed,
            themeColor = prefs[Keys.THEME_COLOR]?.let {
                runCatching { it.toColorType() }.getOrDefault(default.themeColor)
            } ?: default.themeColor,
            themeStyle = prefs[Keys.THEME_STYLE]?.let {
                runCatching { DarkThemeStyle.valueOf(it) }.getOrDefault(default.themeStyle)
            } ?: default.themeStyle,

            freshDays = prefs[Keys.FRESH_DAYS] ?: default.freshDays,
            backgroundPath = prefs[Keys.BACKGROUND_PATH]?.takeIf { it.isNotBlank() }
                ?: default.backgroundPath,
            backgroundAlpha = prefs[Keys.BACKGROUND_ALPHA] ?: default.backgroundAlpha,
            proxy = when (prefs[Keys.PROXY_TYPE]) {
                "http" -> Proxy.HttpProxy(
                    host = prefs[Keys.PROXY_ADDRESS] ?: "localhost",
                    port = prefs[Keys.PROXY_PORT] ?: 8080
                )

                "socks" -> Proxy.SocksProxy(
                    host = prefs[Keys.PROXY_ADDRESS] ?: "localhost",
                    port = prefs[Keys.PROXY_PORT] ?: 8080
                )

                else -> Proxy.NoProxy()
            },
            languageCode = prefs[Keys.LANGUAGE_CODE]?.takeIf { it.isNotBlank() }
                ?: default.languageCode,
            currentTableId = prefs[Keys.CURRENT_TABLE_ID].takeIf { it != 0L } ?: default.currentTableId,
            showTimeline = prefs[Keys.SHOW_TIMELINE] ?: default.showTimeline,
            showDate = prefs[Keys.SHOW_DATE] ?: default.showDate,
            showPeriodTime = prefs[Keys.SHOW_PERIOD_TIME] ?: default.showPeriodTime,
            showNonCurrentWeek = prefs[Keys.SHOW_NON_CURRENT_WEEK] ?: default.showNonCurrentWeek,
            newsNotificationEnabled = prefs[Keys.NEWS_NOTIFICATION_ENABLED] ?: default.newsNotificationEnabled,
            newsCheckIntervalMinutes = prefs[Keys.NEWS_CHECK_INTERVAL_MINUTES] ?: default.newsCheckIntervalMinutes,
            courseReminderEnabled = prefs[Keys.COURSE_REMINDER_ENABLED] ?: default.courseReminderEnabled,
            permissionReminderDismissed = prefs[Keys.PERMISSION_REMINDER_DISMISSED] ?: default.permissionReminderDismissed,
            autoStartEnabled = prefs[Keys.AUTO_START_ENABLED] ?: default.autoStartEnabled,
            dailyReportEnabled = prefs[Keys.DAILY_REPORT_ENABLED] ?: default.dailyReportEnabled,
            dailyReportTime = prefs[Keys.DAILY_REPORT_TIME] ?: default.dailyReportTime,
            crawlDaysGap = prefs[Keys.CRAWL_DAYS_GAP] ?: default.crawlDaysGap,
            mottoText = prefs[Keys.MOTTO_TEXT] ?: default.mottoText,
            mottoAuthor = prefs[Keys.MOTTO_AUTHOR] ?: default.mottoAuthor,
            mottoOnline = prefs[Keys.MOTTO_ONLINE] ?: default.mottoOnline,
            hitokotoCategory = prefs[Keys.HITOKOTO_CATEGORY] ?: default.hitokotoCategory,
            hitokotoMaxLength = prefs[Keys.HITOKOTO_MAX_LENGTH] ?: default.hitokotoMaxLength
        )
    }
    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        context.settingsStore.edit { it[key] = value }
    }

    suspend fun saveColorType(color: ColorType) {
        context.settingsStore.edit { it[Keys.THEME_COLOR] = color.toStorageString() }
    }

    suspend fun saveBackgroundPath(path: String?) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.BACKGROUND_PATH] = path ?: ""
        }
    }

    suspend fun saveProxy(proxy: Proxy) {
        context.settingsStore.edit { prefs ->
            when (proxy) {
                is Proxy.NoProxy -> prefs[Keys.PROXY_TYPE] = ""

                is Proxy.HttpProxy -> {
                    prefs[Keys.PROXY_TYPE] = "http"
                    prefs[Keys.PROXY_ADDRESS] = proxy.host
                    prefs[Keys.PROXY_PORT] = proxy.port
                }

                is Proxy.SocksProxy -> {
                    prefs[Keys.PROXY_TYPE] = "socks"
                    prefs[Keys.PROXY_ADDRESS] = proxy.host
                    prefs[Keys.PROXY_PORT] = proxy.port
                }
            }
        }
    }

    suspend fun saveLanguageCode(code: String?) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = code ?: ""
        }
    }

    /** 记录/取消记录「被删除的内置数据源」。 */
    suspend fun markSourceDeleted(id: String) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.DELETED_SOURCE_IDS] = (prefs[Keys.DELETED_SOURCE_IDS] ?: emptySet()) + id
        }
    }

    suspend fun clearSourceDeleted(id: String) {
        context.settingsStore.edit { prefs ->
            val current = prefs[Keys.DELETED_SOURCE_IDS] ?: return@edit
            if (id in current) prefs[Keys.DELETED_SOURCE_IDS] = current - id
        }
    }

    suspend fun deletedSourceIds(): Set<String> =
        context.settingsStore.data.first()[Keys.DELETED_SOURCE_IDS] ?: emptySet()

    suspend fun getToggleState(id: String): Boolean {
        val key = booleanPreferencesKey("toggle_$id")
        return context.settingsStore.data.first()[key] ?: false
    }

    suspend fun saveToggleState(id: String, value: Boolean) {
        save(booleanPreferencesKey("toggle_$id"), value)
    }
}
