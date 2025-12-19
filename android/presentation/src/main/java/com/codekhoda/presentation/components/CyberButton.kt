package com.codekhoda.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codekhoda.presentation.theme.*

/**
 * 🔘 Cyberpunk-styled Primary Button
 * Animated neon button with glow effects and press feedback
 */
@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glowColor: Color = NeonCyan,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    contentPadding: PaddingValues = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ButtonScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ButtonGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val textColor = when (variant) {
        ButtonVariant.PRIMARY -> DeepBlack
        ButtonVariant.SECONDARY -> glowColor
        ButtonVariant.DANGER -> Color.White
        ButtonVariant.GRADIENT -> Color.White
    }

    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 48.dp)
            .drawBehind {
                if (enabled) {
                    // Outer glow
                    drawRoundRect(
                        color = glowColor.copy(alpha = glowAlpha * 0.4f),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(width = 8.dp.toPx())
                    )
                    
                    // Sharp neon edge
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.9f),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (variant == ButtonVariant.SECONDARY) {
                        listOf(Color.Transparent, Color.Transparent)
                    } else {
                        listOf(glowColor.copy(alpha = 0.9f), glowColor.copy(alpha = 0.7f))
                    }
                ),
                shape = shape
            )
            .then(
                if (variant == ButtonVariant.SECONDARY) {
                    Modifier.border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(glowColor, glowColor.copy(alpha = 0.6f))
                        ),
                        shape = shape
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (enabled) textColor else TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    DANGER,
    GRADIENT
}

/**
 * 🔴 Floating Action Button with Glow
 */
@Composable
fun CyberFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = NeonCyan,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "FabScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "FabGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FabGlowAlpha"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(64.dp)
            .drawBehind {
                drawCircle(
                    color = glowColor.copy(alpha = glowAlpha * 0.5f),
                    radius = size.minDimension / 2 + 16.dp.toPx()
                )
            }
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, glowColor.copy(alpha = 0.7f))
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
