package org.openjwc.client.ui.main

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import org.openjwc.client.R

sealed class MainTab(
    @param: StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Chat : MainTab(R.string.chat, Icons.AutoMirrored.Filled.Chat)
    object News : MainTab(R.string.news, Icons.Filled.Info)
    object Timetable: MainTab(R.string.timetable, Icons.Filled.Info)
    object Me : MainTab(R.string.me, Icons.Filled.Person)
    companion object {
        val items get() = listOf(Chat, News, Timetable, Me)
    }
}