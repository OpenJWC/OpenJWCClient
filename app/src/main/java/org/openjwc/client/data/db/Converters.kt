package org.openjwc.client.data.db

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.TypeConverter
import org.openjwc.client.ui.theme.ColorType
import org.openjwc.client.ui.theme.DarkThemeStyle
import androidx.core.graphics.toColorInt
import org.openjwc.client.ui.theme.SeedDefault

class Converters {
    @TypeConverter
    fun fromDarkThemeStyle(value: DarkThemeStyle) = value.name

    @TypeConverter
    fun toDarkThemeStyle(value: String) = try {
        enumValueOf<DarkThemeStyle>(value)
    } catch (e: Exception) {
        DarkThemeStyle.Auto
    }

    @TypeConverter
    fun fromColorType(value: ColorType) = when (value) {
        is ColorType.Dynamic -> "DYNAMIC"
        is ColorType.Custom -> {
            val argb = value.color.toArgb()
            "CUSTOM:#%08X".format(argb)
        }
    }

    @TypeConverter
    fun toColorType(value: String): ColorType {
        if (value == "DYNAMIC") return ColorType.Dynamic

        return if (value.startsWith("CUSTOM:")) {
            try {
                val colorHex = value.substringAfter("CUSTOM:")
                ColorType.Custom(Color(colorHex.toColorInt()))
            } catch (e: Exception) {
                ColorType.Custom(SeedDefault)
            }
        } else {
            ColorType.Dynamic
        }
    }
}
