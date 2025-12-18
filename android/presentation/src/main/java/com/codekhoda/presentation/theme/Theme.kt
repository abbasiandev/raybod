package com.codekhoda.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00E5FF)
val DeepBlack = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val AlertRed = Color(0xFFFF1744)
val SafeGreen = Color(0xFF00E676)

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = SafeGreen,
    tertiary = AlertRed,
    background = DeepBlack,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun HybridCloudSentinelTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme, // Force Dark Mode for this aesthetic
        content = content
    )
}
