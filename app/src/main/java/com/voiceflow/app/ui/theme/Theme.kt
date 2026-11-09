package com.voiceflow.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    secondary = Color(0xFF188038),
    onSecondary = Color.White,
    error = Color(0xFFD93025),
    background = Color(0xFFF8F9FA),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4A90E2),
    onPrimary = Color.White,
    secondary = Color(0xFF34A853),
    onSecondary = Color.White,
    error = Color(0xFFEA4335),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF2C2C2E),
)

@Composable
fun VoiceFlowTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
