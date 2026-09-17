package org.openjwc.client.navigation

import android.os.Parcelable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Article
import androidx.compose.material.icons.twotone.CalendarMonth
import androidx.compose.material.icons.twotone.Chat
import androidx.compose.material.icons.twotone.Newspaper
import androidx.compose.material.icons.twotone.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey, Parcelable {
    @Serializable
    @Parcelize
    object Settings : Screen
    @Serializable
    @Parcelize
    object Main : Screen
    @Serializable
    @Parcelize
    object About : Screen
    @Serializable
    @Parcelize
    object Theme : Screen
    @Serializable
    @Parcelize
    object ThemeSettings : Screen
    @Serializable
    @Parcelize
    object NewsSettings : Screen
    @Serializable
    @Parcelize
    object NotificationSettings : Screen
    @Serializable
    @Parcelize
    object WidgetSettings : Screen
    @Serializable
    @Parcelize
    object Policy : Screen
    @Serializable
    @Parcelize
    object License : Screen
    @Serializable
    @Parcelize
    object Log : Screen
    @Serializable
    @Parcelize
    object Favorite : Screen
    @Serializable
    @Parcelize
    object NewsDetail : Screen
    /** 从聊天工具卡片打开的资讯详情：不走共享元素，用全局预测返回。 */
    @Serializable
    @Parcelize
    object NoticeDetail : Screen
    @Serializable
    @Parcelize
    object Language : Screen
    @Serializable
    @Parcelize
    object Load : Screen
    @Serializable
    @Parcelize
    object TimetablePrefs : Screen
    @Serializable
    @Parcelize
    object LlmSettings : Screen
    @Serializable
    @Parcelize
    object Sources : Screen
    @Serializable
    @Parcelize
    object MottoSettings : Screen
    @Serializable
    @Parcelize
    object SourceDetail : Screen
    @Serializable
    @Parcelize
    object SourceScript : Screen
    @Serializable
    @Parcelize
    object ImageViewer : Screen
}

enum class MainTab(val titleRes: Int, val iconSelected: ImageVector, val iconNotSelected: ImageVector) {
    Chat(org.openjwc.client.R.string.chat, Icons.TwoTone.Chat, Icons.TwoTone.Chat),
    DailyReport(org.openjwc.client.R.string.daily_report, Icons.AutoMirrored.TwoTone.Article, Icons.AutoMirrored.TwoTone.Article),
    News(org.openjwc.client.R.string.news, Icons.TwoTone.Newspaper, Icons.TwoTone.Newspaper),
    Timetable(org.openjwc.client.R.string.timetable, Icons.TwoTone.CalendarMonth, Icons.TwoTone.CalendarMonth),
    Me(org.openjwc.client.R.string.me, Icons.TwoTone.Person, Icons.TwoTone.Person),
}
