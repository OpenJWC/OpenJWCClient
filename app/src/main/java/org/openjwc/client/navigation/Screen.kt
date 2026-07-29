package org.openjwc.client.navigation

import android.os.Parcelable
import androidx.compose.material.icons.Icons
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
    object UploadNews : Screen
    @Serializable
    @Parcelize
    object About : Screen
    @Serializable
    @Parcelize
    object Host : Screen
    @Serializable
    @Parcelize
    object Login : Screen
    @Serializable
    @Parcelize
    object Register : Screen
    @Serializable
    @Parcelize
    object Account : Screen
    @Serializable
    @Parcelize
    object Theme : Screen
    @Serializable
    @Parcelize
    object ThemeSettings : Screen
    @Serializable
    @Parcelize
    object Review : Screen
    @Serializable
    @Parcelize
    object NewsSettings : Screen
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
    @Serializable
    @Parcelize
    object Language : Screen
    @Serializable
    @Parcelize
    object Load : Screen
    @Serializable
    @Parcelize
    object TimetablePrefs : Screen
}

enum class MainTab(val titleRes: Int, val iconSelected: ImageVector, val iconNotSelected: ImageVector) {
    Chat(org.openjwc.client.R.string.chat, Icons.TwoTone.Chat, Icons.TwoTone.Chat),
    News(org.openjwc.client.R.string.news, Icons.TwoTone.Newspaper, Icons.TwoTone.Newspaper),
    Timetable(org.openjwc.client.R.string.timetable, Icons.TwoTone.CalendarMonth, Icons.TwoTone.CalendarMonth),
    Me(org.openjwc.client.R.string.me, Icons.TwoTone.Person, Icons.TwoTone.Person),
}
