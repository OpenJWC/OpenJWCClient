package org.openjwc.client.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt

import kotlinx.serialization.Serializable

@Serializable
enum class DarkThemeStyle {
    Auto, Light, Dark
}

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
    SeedVibrantRed, SeedDeepOrange, SeedGoldenAmber, SeedForestGreen,
    SeedCyberTeal, SeedBusinessBlue, SeedRoyalPurple, SeedSakuraPink
)

val courseBackgroundColors = listOf(
    Color(0xFFC62828), Color(0xFFAD1457), Color(0xFF6A1B9A), Color(0xFF4527A0),
    Color(0xFF283593), Color(0xFF1565C0), Color(0xFF0277BD), Color(0xFF00838F),
    Color(0xFF00695C), Color(0xFF2E7D32), Color(0xFF558B2F), Color(0xFFFF8F00),
    Color(0xFFEF6C00), Color(0xFFD84315), Color(0xFF4E342E), Color(0xFF37474F)
)

@Composable
fun ColorItem(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .padding(4.dp)
            .aspectRatio(1f)
            .then(
                if (isSelected) Modifier.border(
                    3.dp, MaterialTheme.colorScheme.primary, CircleShape
                ) else Modifier
            )
            .padding(4.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check, contentDescription = null,
                    tint = if (isDarkColor(color)) Color.White else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun isDarkColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance < 0.5
}

object ThemeSeedColors {
    val Default = Color(0xFF415F91)
    val Green = Color(0xFF4C662B)
    val Purple = Color(0xFF7C4E7E)
    val Orange = Color(0xFF8B4F24)
    val Pink = Color(0xFF8C4A60)
    val Gray = Color(0xFF5B5C5C)
    val Yellow = Color(0xFF6D5E0F)
    val Sakura = Color(0xFFB8708C)
    val Blue = Color(0xFF0061A4)
    val Teal = Color(0xFF1E6E6E)

    val all = listOf(Default, Green, Purple, Orange, Pink, Gray, Yellow, Sakura, Blue, Teal)

    fun fromLegacyName(name: String): Color = when (name.lowercase()) {
        "green" -> Green
        "purple" -> Purple
        "orange" -> Orange
        "pink" -> Pink
        "gray" -> Gray
        "yellow" -> Yellow
        "sakura" -> Sakura
        "blue" -> Blue
        "teal" -> Teal
        else -> Default
    }

    fun fromLegacyNameArgb(name: String): Int = fromLegacyName(name).toArgb()
}
