package org.openjwc.client.data

import android.content.Context
import android.content.SharedPreferences

private const val PREF_NAME = "openjwc_theme_prefs"

class AppPreferencesWrapper(private val prefs: SharedPreferences) {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)
    fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)
    fun getFloat(key: String, defaultValue: Float): Float = prefs.getFloat(key, defaultValue)
    fun getString(key: String, defaultValue: String?): String? = prefs.getString(key, defaultValue)
    fun contains(key: String): Boolean = prefs.contains(key)

    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()
    fun remove(key: String) = prefs.edit().remove(key).apply()
}

val Context.appPreferences: AppPreferencesWrapper
    get() = AppPreferencesWrapper(applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE))
