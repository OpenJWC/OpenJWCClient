package org.openjwc.client.data.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import org.openjwc.client.data.models.Role

class Converters {
    @TypeConverter
    fun fromRole(role: Role): String {
        return role.name
    }

    @TypeConverter
    fun toRole(value: String): Role {
        return try {
            Role.valueOf(value)
        } catch (e: IllegalArgumentException) {
            Role.USER
        }
    }

    @TypeConverter
    fun fromAttachmentTitles(value: String): List<String> {
        return try {
            Json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toAttachmentTitles(list: List<String>): String {
        return Json.encodeToString(list)
    }
}
