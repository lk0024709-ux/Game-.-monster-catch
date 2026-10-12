package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ======================================================
// TYPOGRAPHY
// Headings: heavy geometric sans (Manrope stand-in), tight tracking.
// Body: neutral sans (Inter stand-in), comfortable tracking.
// Buttons: semibold, +0.2 tracking.
// ======================================================

private val Heading = FontFamily.SansSerif
private val Body = FontFamily.SansSerif

object RannText {
    val display = TextStyle(
        fontFamily = Heading,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        letterSpacing = (-0.5).sp,
    )
    val h1 = TextStyle(
        fontFamily = Heading,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        letterSpacing = (-0.5).sp,
    )
    val h2 = TextStyle(
        fontFamily = Heading,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = (-0.4).sp,
    )
    val h3 = TextStyle(
        fontFamily = Heading,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = (-0.2).sp,
    )
    val body = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    )
    val bodySmall = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val label = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp,
    )
    val button = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    )
    val buttonSmall = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp,
    )
    val mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.4.sp,
    )
}

val Typography = Typography(
    displayLarge = RannText.display,
    headlineLarge = RannText.h1,
    headlineMedium = RannText.h2,
    titleMedium = RannText.h3,
    bodyLarge = RannText.body,
    bodyMedium = RannText.body,
    bodySmall = RannText.bodySmall,
    labelLarge = RannText.button,
    labelSmall = RannText.label,
)
