package com.notifsync.app.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF08111F),
    primaryContainer = AccentBlueContainer,
    onPrimaryContainer = NightText,
    secondary = AccentTeal,
    onSecondary = Color(0xFF071310),
    secondaryContainer = AccentTealContainer,
    onSecondaryContainer = NightText,
    tertiary = Color(0xFFB8A7FF),
    onTertiary = Color(0xFF11111A),
    tertiaryContainer = Color(0xFF2D2848),
    onTertiaryContainer = NightText,
    background = DeepNight,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightMutedText,
    outline = NightOutline,
    outlineVariant = Color(0xFF2B3441),
    error = Color(0xFFF2A1A1),
    onError = Color(0xFF2B1010),
    errorContainer = Color(0xFF3E1E1E),
    onErrorContainer = Color(0xFFFFDCDC)
)

private val LightColors = lightColorScheme(
    primary = LightAccentBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = LightAccentBlueContainer,
    onPrimaryContainer = LightText,
    secondary = LightAccentTeal,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LightAccentTealContainer,
    onSecondaryContainer = LightText,
    tertiary = Color(0xFF7C68D9),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8E0FF),
    onTertiaryContainer = LightText,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMutedText,
    outline = LightOutline,
    outlineVariant = Color(0xFFD4DAE3),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF7A271A)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
