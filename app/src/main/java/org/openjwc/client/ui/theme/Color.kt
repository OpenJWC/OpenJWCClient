package org.openjwc.client.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt

sealed class ColorType {
    object Dynamic : ColorType()
    class Custom(val color: Color) : ColorType()
}
fun ColorType.toStorageString(): String = when (this) {
    is ColorType.Dynamic -> "DYNAMIC"
    is ColorType.Custom -> "CUSTOM:#%08X".format(this.color.toArgb())
}

fun String?.toColorType(): ColorType {
    if (this == "DYNAMIC" || this == null) return ColorType.Dynamic
    return if (this.startsWith("CUSTOM:")) {
        try {
            val colorHex = this.substringAfter("CUSTOM:")
            ColorType.Custom(Color(colorHex.toColorInt()))
        } catch (e: Exception) {
            ColorType.Custom(SeedDefault)
        }
    } else ColorType.Dynamic
}
val SeedDefault = Color(0xFF6750A4)

val SeedVibrantRed = Color(0xFFB3261E)
val SeedDeepOrange = Color(0xFFBF360C)
val SeedGoldenAmber = Color(0xFF745B00)
val SeedForestGreen = Color(0xFF006E1C)
val SeedCyberTeal = Color(0xFF006A6A)
val SeedBusinessBlue = Color(0xFF005AC1)
val SeedRoyalPurple = Color(0xFF6750A4)
val SeedSakuraPink = Color(0xFF984061)

val seedColors = listOf(
    SeedVibrantRed,
    SeedDeepOrange,
    SeedGoldenAmber,
    SeedForestGreen,
    SeedCyberTeal,
    SeedBusinessBlue,
    SeedRoyalPurple,
    SeedSakuraPink
)
