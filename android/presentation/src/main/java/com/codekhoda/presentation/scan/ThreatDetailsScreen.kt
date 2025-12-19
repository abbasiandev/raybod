package com.codekhoda.presentation.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.presentation.components.*
import com.codekhoda.presentation.theme.*

@Composable
fun ThreatDetailsScreen(
    assessment: RiskAssessment,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlowingCard(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            glowColor = getRiskColor(assessment.riskLevel),
            glowIntensity = 0.5f,
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = getRiskIcon(assessment.riskLevel),
                        contentDescription = "Risk Icon",
                        tint = getRiskColor(assessment.riskLevel),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = assessment.packageName,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = assessment.riskLevel.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = getRiskColor(assessment.riskLevel),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = getRiskColor(assessment.riskLevel).copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(24.dp))

                // Threat Description
                Text(
                    text = "ANALYSIS REPORT",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = assessment.description.ifEmpty { "No specific threat vectors identified." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Heuristics / Permissions
                if (assessment.heuristicsUsed.isNotEmpty()) {
                    Text(
                        text = "DETECTED VECTORS",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(assessment.heuristicsUsed) { vector ->
                            VectorItem(vector, getRiskColor(assessment.riskLevel))
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    if (assessment.riskLevel == RiskLevel.SAFE) {
                        EmptyStateSafe()
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CyberButton(
                        text = "CLOSE",
                        onClick = onDismiss,
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                        glowColor = getRiskColor(assessment.riskLevel)
                    )
                    
                    if (assessment.riskLevel != RiskLevel.SAFE) {
                        CyberButton(
                            text = "UNINSTALL",
                            onClick = { /* TODO: Implement Uninstall */ },
                            variant = ButtonVariant.DANGER,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VectorItem(text: String, color: Color) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = color.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun EmptyStateSafe() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = SafeGreen.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "System Integrity Verified",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

private fun getRiskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.SAFE -> SafeGreen
    RiskLevel.LOW -> NeonCyan
    RiskLevel.MEDIUM -> WarningOrange
    RiskLevel.HIGH -> AlertRed
    RiskLevel.CRITICAL -> CriticalMagenta
    RiskLevel.UNKNOWN -> TextMuted
}

private fun getRiskIcon(level: RiskLevel): ImageVector = when (level) {
    RiskLevel.SAFE -> Icons.Default.CheckCircle
    else -> Icons.Default.Warning
}
