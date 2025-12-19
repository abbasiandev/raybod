package com.codekhoda.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.presentation.components.*
import com.codekhoda.presentation.theme.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@Composable
fun ThreatDetailsScreen(
    assessment: RiskAssessment,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Full screen report style
        StatusCard(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            status = when (assessment.riskLevel) {
                RiskLevel.SAFE -> CardStatus.SAFE
                RiskLevel.LOW -> CardStatus.NEUTRAL
                RiskLevel.MEDIUM -> CardStatus.WARNING
                else -> CardStatus.DANGER
            },
            animated = assessment.riskLevel != RiskLevel.SAFE
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header - Forensic Style
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = getRiskIcon(assessment.riskLevel),
                        contentDescription = "Risk Icon",
                        tint = getRiskColor(assessment.riskLevel),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = assessment.packageName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "THREAT LEVEL: ${assessment.riskLevel.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = getRiskColor(assessment.riskLevel),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "STATUS: ${if (assessment.riskLevel == RiskLevel.SAFE) "CLEAN" else "QUARANTINE RECOMMENDED"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Analysis Details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "DETECTION LOG",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = assessment.description.ifEmpty { "No malicious behavioral patterns detected during static and heuristic analysis." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Forensics List
                Text(
                    text = "DREBIN FORENSICS (AI VECTORS)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        DrebinCategoryView(
                            "S2: REQUESTED PERMISSIONS", 
                            assessment.drebinFeatures.s2RequestedPermissions.filter { it.contains("android.permission.") }
                                .map { it.substringAfterLast('.') },
                            NeonCyan
                        )
                    }
                    item {
                        DrebinCategoryView(
                            "S7: SUSPICIOUS PATTERNS", 
                            assessment.drebinFeatures.s7SuspiciousApis, 
                            NeonPink
                        )
                    }
                    item {
                        DrebinCategoryView(
                            "S5: RESTRICTED APIS", 
                            assessment.drebinFeatures.s5RestrictedApis, 
                            NeonPurple
                        )
                    }
                    item {
                        DrebinCategoryView(
                            "S8: NETWORK INFRASTRUCTURE", 
                            assessment.drebinFeatures.s8NetworkAddresses, 
                            WarningOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CyberButton(
                        text = "DISMISS",
                        onClick = onDismiss,
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                        glowColor = TextSecondary
                    )
                    
                    if (assessment.riskLevel != RiskLevel.SAFE) {
                        CyberButton(
                            text = "UNINSTALL",
                            onClick = { 
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = Uri.parse("package:${assessment.packageName}")
                                }
                                context.startActivity(intent)
                                onDismiss()
                            },
                            variant = ButtonVariant.DANGER,
                            modifier = Modifier.weight(1.5f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getRiskColor(riskLevel: RiskLevel): Color {
    return when (riskLevel) {
        RiskLevel.SAFE -> SafeGreen
        RiskLevel.LOW -> NeonCyan
        RiskLevel.MEDIUM -> WarningOrange
        RiskLevel.HIGH -> NeonPink
        RiskLevel.CRITICAL -> CriticalMagenta
        RiskLevel.UNKNOWN -> TextSecondary
    }
}

@Composable
private fun getRiskIcon(riskLevel: RiskLevel): ImageVector {
    return when (riskLevel) {
        RiskLevel.SAFE -> Icons.Default.CheckCircle
        RiskLevel.LOW -> Icons.Default.Info
        RiskLevel.MEDIUM -> Icons.Default.Warning
        RiskLevel.HIGH -> Icons.Default.Warning
        RiskLevel.CRITICAL -> Icons.Default.Warning
        RiskLevel.UNKNOWN -> Icons.Default.Info
    }
}

@Composable
fun DrebinCategoryView(title: String, features: List<String>, color: Color) {
    if (features.isEmpty()) return
    
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        features.forEach { feature ->
            VectorItem(feature, color)
        }
    }
}

@Composable
fun VectorItem(text: String, color: Color) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        cornerRadius = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = color.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary.copy(alpha = 0.9f)
            )
        }
    }
}
