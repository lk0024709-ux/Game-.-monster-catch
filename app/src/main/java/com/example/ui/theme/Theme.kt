package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LocalRannColors = staticCompositionLocalOf { LightRannColors }

/** Access point for theme tokens: `RannTheme.colors.surface` etc. */
object RannTheme {
    val colors: RannColors
        @Composable @ReadOnlyComposable get() = LocalRannColors.current
}

private val LightScheme = lightColorScheme(
    primary = Rann.Primary,
    onPrimary = Color.White,
    primaryContainer = Rann.GoldSoft,
    onPrimaryContainer = Rann.TextPrimary,
    secondary = Rann.Secondary,
    onSecondary = Color.White,
    tertiary = Rann.Accent,
    onTertiary = Color.White,
    background = Rann.Cream,
    onBackground = Rann.TextPrimary,
    surface = Rann.Surface,
    onSurface = Rann.TextPrimary,
    surfaceVariant = Rann.SurfaceAlt,
    onSurfaceVariant = Rann.TextMuted,
    outline = Rann.Border,
    error = Rann.Danger,
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = Rann.Primary,
    onPrimary = Color.White,
    primaryContainer = Rann.DarkSurfaceAlt,
    onPrimaryContainer = Rann.DarkText,
    secondary = Rann.Secondary,
    onSecondary = Color.White,
    tertiary = Rann.Accent,
    onTertiary = Color.White,
    background = Rann.DarkBg,
    onBackground = Rann.DarkText,
    surface = Rann.DarkSurface,
    onSurface = Rann.DarkText,
    surfaceVariant = Rann.DarkSurfaceAlt,
    onSurfaceVariant = Rann.DarkTextMuted,
    outline = Rann.DarkBorder,
    error = Rann.Danger,
    onError = Color.White,
)

/**
 * Root theme. Static colour schemes only (no dynamic colour) so the
 * premium warm palette is identical on every device.
 *
 * [forceDark] is used by Elite Mode to switch to the exclusive dark palette,
 * independent of the system setting.
 */
@Composable
fun RannbhoomiTheme(
    forceDark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val dark = forceDark ?: isSystemInDarkTheme()
    val tokens = if (dark) DarkRannColors else LightRannColors
    CompositionLocalProvider(LocalRannColors provides tokens) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = Typography,
            content = content,
        )
    }
}

/**
 * Compatibility entry point kept for the original screenshot test and any
 * legacy screens that still call the old name.
 */
@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    RannbhoomiTheme(content = content)
}
