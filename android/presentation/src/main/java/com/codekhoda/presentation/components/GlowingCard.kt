package com.codekhoda.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codekhoda.presentation.theme.*

/**
 * 🌟 Glowing Card Component
 * A card with animated neon glow effect for the cyberpunk aesthetic
 */
@Composable
fun GlowingCard(
    modifier: Modifier = Modifier,
    glowColor: Color = NeonCyan,
    glowIntensity: Float = 0.3f,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 16.dp,
    animated: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GlowPulse")
    
    val animatedAlpha by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = glowIntensity * 0.5f,
            targetValue = glowIntensity,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowAlpha"
        )
    } else {
        remember { mutableFloatStateOf(glowIntensity) }
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                // Outer glow effect
                drawRoundRect(
                    color = glowColor.copy(alpha = animatedAlpha * 0.2f),
                    cornerRadius = CornerRadius(cornerRadius.toPx() + 8.dp.toPx()),
                    style = Stroke(width = 12.dp.toPx())
                )
                // Inner glow
                drawRoundRect(
                    color = glowColor.copy(alpha = animatedAlpha * 0.4f),
                    cornerRadius = CornerRadius(cornerRadius.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ElevatedSurface,
                        CardSurface
                    )
                ),
                shape = shape
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.5f),
                        glowColor.copy(alpha = 0.1f),
                        glowColor.copy(alpha = 0.4f)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

/**
 * 🔲 Glass Morphism Card
 * Frosted glass effect for modern UI elements
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

/**
 * 🎯 Status Card with color-coded glow
 */
@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    status: CardStatus,
    animated: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val glowColor = when (status) {
        CardStatus.SAFE -> SafeGreen
        CardStatus.WARNING -> WarningOrange
        CardStatus.DANGER -> AlertRed
        CardStatus.CRITICAL -> CriticalMagenta
        CardStatus.NEUTRAL -> NeonCyan
        CardStatus.SCANNING -> NeonPurple
    }

    GlowingCard(
        modifier = modifier,
        glowColor = glowColor,
        animated = animated,
        content = content
    )
}

enum class CardStatus {
    SAFE,
    WARNING,
    DANGER,
    CRITICAL,
    NEUTRAL,
    SCANNING
}
