package com.codekhoda.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════
// 🎨 CYBERPUNK COLOR PALETTE - "NEON NOIR"
// ═══════════════════════════════════════════════════════════════════

// Primary Neon Colors
val NeonCyan = Color(0xFF00E5FF)
val NeonPink = Color(0xFFFF0080)
val NeonPurple = Color(0xFFBF00FF)
val NeonBlue = Color(0xFF0066FF)
val ElectricViolet = Color(0xFF8B00FF)

// Status Colors
val SafeGreen = Color(0xFF00E676)
val WarningOrange = Color(0xFFFF9100)
val AlertRed = Color(0xFFFF1744)
val CriticalMagenta = Color(0xFFFF0055)

// Background & Surface
val DeepBlack = Color(0xFF0A0A0F)
val DarkSurface = Color(0xFF12121A)
val CardSurface = Color(0xFF1A1A25)
val ElevatedSurface = Color(0xFF222233)
val GlassSurface = Color(0x1AFFFFFF)

// Text Colors
val TextPrimary = Color(0xFFEEEEFF)
val TextSecondary = Color(0xFFAAB4BE)
val TextMuted = Color(0xFF6B7280)

// Gradient Colors
val GradientStart = Color(0xFF00E5FF)
val GradientMiddle = Color(0xFF8B00FF)
val GradientEnd = Color(0xFFFF0080)

// Glow Effects
val CyanGlow = Color(0x4000E5FF)
val PinkGlow = Color(0x40FF0080)
val PurpleGlow = Color(0x40BF00FF)

// ═══════════════════════════════════════════════════════════════════
// 📝 TYPOGRAPHY - FUTURISTIC SYSTEM
// ═══════════════════════════════════════════════════════════════════

val CyberpunkTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = (-1).sp,
        lineHeight = 56.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = (-0.5).sp,
        lineHeight = 44.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = 0.sp,
        lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 1.25.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        lineHeight = 14.sp
    )
)

// ═══════════════════════════════════════════════════════════════════
// 🌙 DARK COLOR SCHEME
// ═══════════════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonPink,
    tertiary = NeonPurple,
    background = DeepBlack,
    surface = DarkSurface,
    surfaceVariant = CardSurface,
    onPrimary = DeepBlack,
    onSecondary = DeepBlack,
    onTertiary = DeepBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = AlertRed,
    onError = Color.White
)

// ═══════════════════════════════════════════════════════════════════
// 🎭 THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════════

@Composable
fun HybridCloudSentinelTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = CyberpunkTypography,
        content = content
    )
}
