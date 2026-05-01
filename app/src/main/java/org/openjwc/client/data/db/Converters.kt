package org.openjwc.client.data.db

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import org.openjwc.client.data.models.Role
import org.openjwc.client.data.models.SemesterConfig
import org.openjwc.client.log.Logger
import java.time.DayOfWeek

class Converters {
    private val tag = "Converters"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Role
    @TypeConverter
    fun fromRole(role: Role): String = role.name

    @TypeConverter
    fun toRole(value: String): Role = try {
        Role.valueOf(value)
    } catch (e: Exception) {
        Role.USER
    }

    // List<String> & Set<Int> (JSON 存储)
    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toStringList(value: String?): List<String>? = value?.let {
        try { json.decodeFromString<List<String>>(it) } catch (e: Exception) { null }
    }

    @TypeConverter
    fun fromIntSet(value: Set<Int>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toIntSet(value: String?): Set<Int>? = value?.let {
        try { json.decodeFromString<Set<Int>>(it) } catch (e: Exception) { null }
    }

    // DayOfWeek (映射为 Int)
    @TypeConverter
    fun fromDayOfWeek(day: DayOfWeek?): Int? = day?.value

    @TypeConverter
    fun toDayOfWeek(value: Int?): DayOfWeek? = value?.let { DayOfWeek.of(it) }

    // Color (映射为 Int ARGB)
    @TypeConverter
    fun fromColor(color: Color?): Int? = color?.toArgb()

    @TypeConverter
    fun toColor(value: Int?): Color? = value?.let { Color(it) }

    // SemesterConfig (映射为 JSON String)
    @TypeConverter
    fun fromSemesterConfig(config: SemesterConfig?): String? = config?.let { json.encodeToString(it) }

    @TypeConverter
    fun toSemesterConfig(value: String?): SemesterConfig? = value?.let {
        try {
            json.decodeFromString<SemesterConfig>(it)
        } catch (e: Exception) {
            Logger.e(tag, "Failed to decode SemesterConfig: ${e.localizedMessage}")
            null
        }
    }
}
