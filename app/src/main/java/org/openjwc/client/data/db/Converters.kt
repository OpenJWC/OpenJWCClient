package org.openjwc.client.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import org.openjwc.client.data.models.Role
import org.openjwc.client.log.Logger

class Converters {
    private val tag = "Converters"
    @TypeConverter
    fun fromRole(role: Role): String {
        return role.name
    }

    @TypeConverter
    fun toRole(value: String): Role {
        return try {
            Role.valueOf(value)
        } catch (e: IllegalArgumentException) {
            Logger.e(tag, e.localizedMessage ?: "Unknown Error")
            Role.USER
        }
    }

    @TypeConverter
    fun fromAttachmentTitles(value: String): List<String> {
        return try {
            Json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            Logger.e(tag, e.localizedMessage ?: "Unknown Error")
            emptyList()
        }
    }

    @TypeConverter
    fun toAttachmentTitles(list: List<String>): String {
        return Json.encodeToString(list)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        return list?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toList(value: String?): List<String>? {
        return value?.let {
            try {
                json.decodeFromString<List<String>>(it)
            } catch (e: Exception) {
                Logger.e(tag, e.localizedMessage ?: "Unknown Error")
                null
            }
        }
    }
}
