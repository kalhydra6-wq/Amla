package com.amala.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
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

private fun luminance(color: Color): Float =
    color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f

private fun readableOn(color: Color): Color =
    if (luminance(color) > 0.55f) Color(0xFF14181B) else Color(0xFFFFFFFF)

/** Lightens (dark theme) or darkens (light theme) until text/controls stay readable. */
internal fun ensureContrast(color: Color, dark: Boolean): Color {
    var result = color
    val target = if (dark) Color.White else Color.Black
    repeat(10) {
        val backgroundLuma = if (dark) luminance(DeepCharcoal) else 0.95f
        if (kotlin.math.abs(luminance(result) - backgroundLuma) >= 0.32f) return@repeat
        result = result.mix(target, 0.12f)
    }
    return result
}

@Composable
private fun animatedScheme(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = animateColorAsState(target.primary, tween(300)).value,
        onPrimary = animateColorAsState(target.onPrimary, tween(300)).value,
        primaryContainer = animateColorAsState(target.primaryContainer, tween(300)).value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, tween(300)).value,
        secondary = animateColorAsState(target.secondary, tween(300)).value,
        onSecondary = animateColorAsState(target.onSecondary, tween(300)).value,
        tertiary = animateColorAsState(target.tertiary, tween(300)).value,
        background = animateColorAsState(target.background, tween(300)).value,
        surface = animateColorAsState(target.surface, tween(300)).value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, tween(300)).value,
        onBackground = animateColorAsState(target.onBackground, tween(300)).value,
        onSurface = animateColorAsState(target.onSurface, tween(300)).value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, tween(300)).value,
        error = animateColorAsState(target.error, tween(300)).value,
        errorContainer = animateColorAsState(target.errorContainer, tween(300)).value,
        onErrorContainer = animateColorAsState(target.onErrorContainer, tween(300)).value,
    )
}

@Composable
fun AmlaTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    accentColor: Int = 0xFF4C9A2A.toInt(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.FOLLOW_SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val amoled = themeMode == ThemeMode.AMOLED

    val accent = ensureContrast(Color(accentColor), dark)
    val brightAccent = accent.mix(Color.White, 0.28f)
    val container = accent.mix(if (dark) Color.Black else Color.White, if (dark) 0.78f else 0.88f)
    val onContainer = if (dark) brightAccent else accent.mix(Color.Black, 0.4f)

    val scheme = if (dark) {
        darkColorScheme(
            primary = brightAccent,
            onPrimary = readableOn(brightAccent),
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = brightAccent.mix(AccentBlue, 0.45f),
            onSecondary = readableOn(brightAccent),
            tertiary = brightAccent.mix(AccentPurple, 0.5f),
            background = if (amoled) Color.Black else DeepCharcoal,
            surface = if (amoled) Color.Black else DarkCard,
            surfaceVariant = if (amoled) Color(0xFF121212) else DarkSurfaceVariant,
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
            tertiary = accent.mix(AmlaPurple, 0.5f),
            background = SoftCream,
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
        colorScheme = animatedScheme(scheme),
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
