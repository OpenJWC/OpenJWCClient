package org.openjwc.client.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    object Settings : Screen
    @Serializable
    object Main : Screen
    @Serializable
    object UploadNews : Screen
    @Serializable
    object About : Screen
    @Serializable
    object Host : Screen
    @Serializable
    object Login : Screen
    @Serializable
    object Register : Screen
    @Serializable
    object Account : Screen
    @Serializable
    object Theme : Screen
    @Serializable
    object Review : Screen
    @Serializable
    object NewsSettings : Screen
    @Serializable
    object Policy : Screen
    @Serializable
    object License: Screen
    @Serializable
    object Log: Screen
    @Serializable
    object Favorite: Screen
    @Serializable
    object NewsDetail : Screen
}