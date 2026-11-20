package com.amala.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = AccentTeal,
    secondary = AccentBlue,
    onSecondary = DarkBackground,
    tertiary = AccentPurple,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color(0xFFE6EDF7),
    onSurface = Color(0xFFE6EDF7),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007C70),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F5E9),
    onPrimaryContainer = Color(0xFF00564D),
    secondary = Color(0xFF2957A8),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF6D4BC4),
    background = LightBackground,
    surface = LightSurface,
    onBackground = Color(0xFF10192B),
    onSurface = Color(0xFF10192B),
)

@Composable
fun AmlaTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        content = content,
    )
}
