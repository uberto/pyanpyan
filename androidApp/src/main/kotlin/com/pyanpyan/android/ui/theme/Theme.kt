package com.pyanpyan.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB4D5A8),      // Soft green
    secondary = Color(0xFFFFC5A8),     // Soft orange
    tertiary = Color(0xFFA8D5E3),      // Soft blue
    background = Color(0xFFFFFBF5),    // Warm white
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF2D4A26),
    onSecondary = Color(0xFF4A2D1F),
    onTertiary = Color(0xFF1F3A4A),
    onBackground = Color(0xFF3A3A3A),
    onSurface = Color(0xFF3A3A3A)
)

@Composable
fun PyanpyanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Currently only light theme (calm, soft palette for ADHD users)
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
