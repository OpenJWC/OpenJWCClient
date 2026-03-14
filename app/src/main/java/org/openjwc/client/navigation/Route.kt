package org.openjwc.client.navigation

object Routes {
    const val MAIN = "main"
    const val ME = "me"
    const val ABOUT = "about"
    const val SETTINGS_PATTERN = "settings?route={route}"
    fun buildSettingsRoute(routeValue: String): String {
        return "settings?route=$routeValue"
    }
}

