package com.amala.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.amala.app.model.ThemeMode

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun Color.mix(other: Color, fraction: Float): Color = lerp(this, other, fraction)

private fun readableOn(color: Color): Color =
    if (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f > 0.55f) {
        Color(0xFF0B1020)
    } else {
        Color(0xFFFFFFFF)
    }

@Composable
fun AmlaTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    accentColor: Int = 0xFF007C70.toInt(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.FOLLOW_SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val amoled = themeMode == ThemeMode.AMOLED

    val accent = Color(accentColor)
    val brightAccent = accent.mix(Color.White, 0.28f)
    val container = accent.mix(if (dark) Color.Black else Color.White, if (dark) 0.72f else 0.86f)
    val onContainer = if (dark) brightAccent else accent.mix(Color.Black, 0.35f)

    val colors = if (dark) {
        darkColorScheme(
            primary = brightAccent,
            onPrimary = readableOn(brightAccent),
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = brightAccent.mix(Color(0xFF60A5FA), 0.45f),
            onSecondary = readableOn(brightAccent),
            tertiary = brightAccent.mix(Color(0xFFA78BFA), 0.5f),
            background = if (amoled) Color.Black else Color(0xFF0B1020),
            surface = if (amoled) Color.Black else Color(0xFF141B2E),
            surfaceVariant = if (amoled) Color(0xFF121212) else Color(0xFF1D2740),
            onBackground = Color(0xFFE6EDF7),
            onSurface = Color(0xFFE6EDF7),
            onSurfaceVariant = Color(0xFFAAB6C9),
            error = Color(0xFFFF8A80),
            errorContainer = Color(0xFF5C1B1B),
            onErrorContainer = Color(0xFFFFD4D0),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = readableOn(accent),
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = accent.mix(Color(0xFF2957A8), 0.45f),
            onSecondary = Color.White,
            tertiary = accent.mix(Color(0xFF6D4BC4), 0.5f),
            background = Color(0xFFF4F7FB),
            surface = Color.White,
            surfaceVariant = Color(0xFFE8EEF6),
            onBackground = Color(0xFF10192B),
            onSurface = Color(0xFF10192B),
            onSurfaceVariant = Color(0xFF536078),
            error = Color(0xFFB3261E),
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
