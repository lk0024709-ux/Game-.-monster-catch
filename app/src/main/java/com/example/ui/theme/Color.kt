package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ======================================================
// BLADE BRAWL: RANNBHOOMI — DESIGN TOKENS
// Warm, muted, editorial palette. No neon. Ever.
// ======================================================

object Rann {
    // ---- Light mode surfaces ----
    val Cream = Color(0xFFF5F1EA)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFEDE7DB)
    val Border = Color(0xFFE7E1D3)

    // ---- Brand ----
    val Primary = Color(0xFFD97706)
    val PrimaryDark = Color(0xFFB45309)
    val Secondary = Color(0xFF3F6212)
    val Accent = Color(0xFF7C2D12)

    // ---- Text ----
    val TextPrimary = Color(0xFF1C1917)
    val TextMuted = Color(0xFF78716C)

    // ---- Semantic ----
    val Success = Color(0xFF15803D)
    val Danger = Color(0xFFB91C1C)

    // ---- Dark mode (Elite Mode exclusive) ----
    val DarkBg = Color(0xFF1C1917)
    val DarkSurface = Color(0xFF26241F)
    val DarkSurfaceAlt = Color(0xFF2E2B25)
    val DarkBorder = Color(0xFF3A372F)
    val DarkText = Color(0xFFF5F5F4)
    val DarkTextMuted = Color(0xFFA8A29E)

    // ---- Rank colors (muted) ----
    val RankBronze = Color(0xFFA16207)
    val RankSilver = Color(0xFF94A3B8)
    val RankGold = Color(0xFFD97706)
    val RankPlatinum = Color(0xFF0891B2)
    val RankDiamond = Color(0xFF6366F1)
    val RankMasters = Color(0xFF7C2D12)
    val RankGrandMasters = Color(0xFF1C1917)

    // ---- Grade colors ----
    val GradeSS = Color(0xFFD97706)
    val GradeS = Color(0xFF7E22CE)
    val GradeA = Color(0xFF0891B2)
    val GradeB = Color(0xFF65A30D)
    val GradeC = Color(0xFFA16207)
    val GradeD = Color(0xFF78716C)
    val GradeNone = Color(0xFFA8A29E)

    // ---- Battle world tints ----
    val SlotEmpty = Color(0xFFE4DCCC)
    val Gold = Color(0xFFE0A82E)
    val GoldSoft = Color(0xFFF3D9A4)
    val ShadowSoft = Color(0x14000000)

    fun rank(index: Int): Color = when (index) {
        0 -> RankBronze
        1 -> RankSilver
        2 -> RankGold
        3 -> RankPlatinum
        4 -> RankDiamond
        5 -> RankMasters
        else -> RankGrandMasters
    }
}

// Theme aware token bundle so screens never hardcode light/dark values.
data class RannColors(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val primary: Color = Rann.Primary,
    val primaryDark: Color = Rann.PrimaryDark,
    val secondary: Color = Rann.Secondary,
    val accent: Color = Rann.Accent,
    val success: Color = Rann.Success,
    val danger: Color = Rann.Danger,
    val gold: Color = Rann.Gold,
    val isDark: Boolean = false,
)

val LightRannColors = RannColors(
    background = Rann.Cream,
    surface = Rann.Surface,
    surfaceAlt = Rann.SurfaceAlt,
    border = Rann.Border,
    textPrimary = Rann.TextPrimary,
    textMuted = Rann.TextMuted,
    isDark = false,
)

val DarkRannColors = RannColors(
    background = Rann.DarkBg,
    surface = Rann.DarkSurface,
    surfaceAlt = Rann.DarkSurfaceAlt,
    border = Rann.DarkBorder,
    textPrimary = Rann.DarkText,
    textMuted = Rann.DarkTextMuted,
    isDark = true,
)
