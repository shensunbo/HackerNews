package com.example.hackernews.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TerminalScheme = darkColorScheme(
    primary = TerminalColors.Primary,
    onPrimary = TerminalColors.Bg,
    background = TerminalColors.Bg,
    onBackground = TerminalColors.TextPrimary,
    surface = TerminalColors.Surface,
    onSurface = TerminalColors.TextPrimary,
    surfaceVariant = TerminalColors.SurfaceElevated,
    outline = TerminalColors.Border,
    error = TerminalColors.Error,
)

@Composable
fun HackerNewsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TerminalScheme, typography = AppTypography, content = content)
}
