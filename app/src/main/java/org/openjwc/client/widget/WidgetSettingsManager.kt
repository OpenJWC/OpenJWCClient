package org.openjwc.client.widget

import android.content.Context

object WidgetSettingsManager {
    private const val PREFS_NAME = "widget_settings"
    private const val KEY_BG_PATH = "background_image_path"
    private const val KEY_OPACITY = "background_opacity"

    fun getBackgroundImagePath(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BG_PATH, null)
    }

    fun setBackgroundImagePath(context: Context, path: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BG_PATH, path)
            .apply()
    }

    /** 背景不透明度，0..255，255 表示背景图完全可见。 */
    fun getBackgroundOpacity(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_OPACITY, 128)
    }

    fun setBackgroundOpacity(context: Context, opacity: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_OPACITY, opacity.coerceIn(0, 255))
            .apply()
    }
}
