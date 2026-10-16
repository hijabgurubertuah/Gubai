package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = Color(0xFF003734),
    primaryContainer = TealPrimary,
    onPrimaryContainer = TealContainer,
    secondary = TealAccent,
    onSecondary = Color(0xFF003734),
    background = TealDarkBg,
    onBackground = TealTextLight,
    surface = TealDarkSurface,
    onSurface = TealTextLight,
    surfaceVariant = Color(0xFF134E4A),
    onSurfaceVariant = Color(0xFF99F6E4),
    outline = Color(0xFF2DD4BF)
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealOnContainer,
    secondary = TealAccent,
    onSecondary = Color.White,
    background = TealLightBg,
    onBackground = TealTextDark,
    surface = TealLightSurface,
    onSurface = TealTextDark,
    surfaceVariant = Color(0xFFE6FFFA),
    onSurfaceVariant = Color(0xFF115E59),
    outline = Color(0xFF5EEAD4)
)

@Composable
fun GubAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
