package org.openjwc.client.ui.theme

import androidx.compose.ui.graphics.Color

sealed class ColorType {
    object Dynamic : ColorType()
    class Custom(val color: Color) : ColorType()
}

val SeedDefault = Color(0xFF6750A4)
val SeedBusinessBlue = Color(0xFF005AC1)
val SeedForestGreen = Color(0xFF006E1C)
val SeedVividOrange = Color(0xFF8B5000)
val SeedDeepGrey = Color(0xFF445E91)

val seedColors = listOf(
    SeedDefault,
    SeedBusinessBlue,
    SeedForestGreen,
    SeedVividOrange,
    SeedDeepGrey
)