package dev.abbasian.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abbasian.presentation.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * 📡 Advanced Radar Visualization
 * Multi-layered radar with scanning beam, pulse effects, and grid lines
 */
@Composable
fun RadarVisualization(
    modifier: Modifier = Modifier,
    isScanning: Boolean = false,
    progress: Float = 0f,
    scanPhase: ScanPhase = ScanPhase.IDLE,
    threatCount: Int = 0
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar")

    // Slow background rotation for the entire radar
    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "BaseRotation"
    )

    // Main rotation for scanning beam
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "Rotation"
    )

    // Pulse effect for rings
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Glow intensity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    val phaseColor = when (scanPhase) {
        ScanPhase.IDLE -> NeonCyan
        ScanPhase.EXTRACTING -> NeonPurple
        ScanPhase.ANALYZING -> NeonBlue
        ScanPhase.VERDICT -> if (threatCount > 0) AlertRed else SafeGreen
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 20.dp.toPx()

            // Background glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        phaseColor.copy(alpha = glowAlpha * 0.25f),
                        phaseColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.6f
                ),
                radius = radius * 1.6f,
                center = center
            )

            // Outer ring border (high tech look)
            drawCircle(
                color = phaseColor.copy(alpha = 0.4f),
                radius = radius + 4.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Grid lines (crosshairs) with base rotation
            rotate(baseRotation, pivot = center) {
                val lineAlpha = 0.25f
                drawLine(
                    color = phaseColor.copy(alpha = lineAlpha),
                    start = Offset(center.x - radius, center.y),
                    end = Offset(center.x + radius, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = phaseColor.copy(alpha = lineAlpha),
                    start = Offset(center.x, center.y - radius),
                    end = Offset(center.x, center.y + radius),
                    strokeWidth = 1.dp.toPx()
                )

                // Diagonal grid lines
                val diagonalOffset = radius * 0.707f // cos(45°)
                drawLine(
                    color = phaseColor.copy(alpha = lineAlpha * 0.6f),
                    start = Offset(center.x - diagonalOffset, center.y - diagonalOffset),
                    end = Offset(center.x + diagonalOffset, center.y + diagonalOffset),
                    strokeWidth = 0.5.dp.toPx()
                )
                drawLine(
                    color = phaseColor.copy(alpha = lineAlpha * 0.6f),
                    start = Offset(center.x + diagonalOffset, center.y - diagonalOffset),
                    end = Offset(center.x - diagonalOffset, center.y + diagonalOffset),
                    strokeWidth = 0.5.dp.toPx()
                )
            }

            // Concentric rings
            val ringCount = 4
            for (i in 1..ringCount) {
                val ringRadius = radius * (i / ringCount.toFloat())
                val alpha = 0.15f + (i * 0.05f)
                
                drawCircle(
                    color = phaseColor.copy(alpha = alpha),
                    radius = ringRadius * pulseScale,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Outer ring with glow
            drawCircle(
                color = phaseColor.copy(alpha = 0.6f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Scanning beam
            if (isScanning) {
                val sweepAngle = 60f
                rotate(rotation, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to Color.Transparent,
                            0.1f to phaseColor.copy(alpha = 0.8f),
                            0.16f to phaseColor.copy(alpha = 0.3f),
                            0.17f to Color.Transparent,
                            1f to Color.Transparent
                        ),
                        startAngle = 0f,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                }

                // Scanning line
                rotate(rotation, pivot = center) {
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                phaseColor.copy(alpha = 0.8f),
                                phaseColor.copy(alpha = 0.1f)
                            ),
                            start = center,
                            end = Offset(center.x + radius, center.y)
                        ),
                        start = center,
                        end = Offset(center.x + radius, center.y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Progress arc (outer ring fill)
            if (progress > 0f) {
                drawArc(
                    color = phaseColor.copy(alpha = 0.6f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius - 8.dp.toPx(), center.y - radius - 8.dp.toPx()),
                    size = Size((radius + 8.dp.toPx()) * 2, (radius + 8.dp.toPx()) * 2),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Subtle center glow (replaces solid dot to avoid text overlap)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(phaseColor.copy(alpha = 0.2f), Color.Transparent),
                    center = center,
                    radius = 24.dp.toPx()
                ),
                radius = 24.dp.toPx(),
                center = center
            )

            // Threat indicators (decorative dots on rings)
            if (threatCount > 0 && scanPhase == ScanPhase.VERDICT) {
                val dotRadius = 6.dp.toPx()
                repeat(minOf(threatCount, 5)) { i ->
                    val angle = Math.toRadians((i * 72 + 15).toDouble())
                    val dotCenter = Offset(
                        center.x + (radius * 0.6f * cos(angle)).toFloat(),
                        center.y + (radius * 0.6f * sin(angle)).toFloat()
                    )
                    drawCircle(
                        color = AlertRed,
                        radius = dotRadius,
                        center = dotCenter
                    )
                    // Threat dot glow
                    drawCircle(
                        color = AlertRed.copy(alpha = 0.3f),
                        radius = dotRadius * 2,
                        center = dotCenter
                    )
                }
            }
        }

        // Center Status Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp) // Move text further down to avoid any dot overlap
        ) {
            Text(
                text = when (scanPhase) {
                    ScanPhase.IDLE -> "STANDBY"
                    ScanPhase.EXTRACTING -> "EXTRACTING"
                    ScanPhase.ANALYZING -> "ANALYZING"
                    ScanPhase.VERDICT -> if (threatCount > 0) "THREATS" else "SECURE"
                },
                style = MaterialTheme.typography.labelSmall,
                color = phaseColor.copy(alpha = 0.9f),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            if (isScanning) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black
                )
            } else if (scanPhase == ScanPhase.VERDICT) {
                Text(
                    text = if (threatCount > 0) "$threatCount" else "✓",
                    style = MaterialTheme.typography.headlineLarge,
                    color = phaseColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp
                )
            }
        }
    }
}

enum class ScanPhase {
    IDLE,
    EXTRACTING,
    ANALYZING,
    VERDICT
}

/**
 * 🔋 Mini Status Indicator
 * Small animated indicator for list items
 */
@Composable
fun StatusIndicator(
    modifier: Modifier = Modifier,
    status: IndicatorStatus = IndicatorStatus.NEUTRAL
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StatusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val color = when (status) {
        IndicatorStatus.SAFE -> SafeGreen
        IndicatorStatus.WARNING -> WarningOrange
        IndicatorStatus.DANGER -> AlertRed
        IndicatorStatus.NEUTRAL -> NeonCyan
        IndicatorStatus.SCANNING -> NeonPurple
    }

    Canvas(modifier = modifier.size(12.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        
        // Glow
        drawCircle(
            color = color.copy(alpha = pulseAlpha * 0.3f),
            radius = size.minDimension / 2 * 1.5f,
            center = center
        )
        
        // Core dot
        drawCircle(
            color = color,
            radius = size.minDimension / 2,
            center = center
        )
    }
}

enum class IndicatorStatus {
    SAFE,
    WARNING,
    DANGER,
    NEUTRAL,
    SCANNING
}
