package com.notifsync.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7AB7FF),
    secondary = Color(0xFFB8C7E0),
    tertiary = Color(0xFFD0BCFF),
    background = Color(0xFF111318),
    surface = Color(0xFF111318)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B57D0),
    secondary = Color(0xFF5C6470),
    tertiary = Color(0xFF855CCF),
    background = Color(0xFFF8F9FD),
    surface = Color(0xFFF8F9FD)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
