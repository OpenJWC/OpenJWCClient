package org.openjwc.client.ui.theme

import androidx.compose.ui.graphics.Color

sealed class ColorType {
    object Dynamic : ColorType()
    class Custom(val color: Color) : ColorType()
}

val SeedDefault = Color(0xFF6750A4)

val SeedVibrantRed = Color(0xFFB3261E)     // 强烈红：警示、热情（暖色极值）
val SeedDeepOrange = Color(0xFFBF360C)     // 活力橙：动感、提醒
val SeedGoldenAmber = Color(0xFF745B00)    // 琥珀金：高级、警告
val SeedForestGreen = Color(0xFF006E1C)    // 森林绿：自然、安全（深绿）
val SeedCyberTeal = Color(0xFF006A6A)      // 赛博青：清冷、科技（绿蓝交界）
val SeedBusinessBlue = Color(0xFF005AC1)   // 商务蓝：稳重、信任（标准冷色）
val SeedRoyalPurple = Color(0xFF6750A4)    // 皇家紫：神秘、高贵（冷暖中性）
val SeedSakuraPink = Color(0xFF984061)     // 樱花粉：柔和、感性

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
