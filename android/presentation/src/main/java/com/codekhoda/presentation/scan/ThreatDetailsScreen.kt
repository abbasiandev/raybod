package com.codekhoda.presentation.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f),
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

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = getRiskColor(assessment.riskLevel).copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                // DREBIN Explainability
                Text(
                    text = "DREBIN FORENSICS (EXPLAINABILITY)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        DrebinCategoryView(
                            "S2: Requested Permissions", 
                            assessment.drebinFeatures.s2RequestedPermissions.filter { it.contains("android.permission.") }
                                .map { it.substringAfterLast('.') },
                            NeonCyan
                        )
                    }
                    item {
                        DrebinCategoryView(
                            "S7: Suspicious Patterns", 
                            assessment.drebinFeatures.s7SuspiciousApis, 
                            NeonPink
                        )
                    }
                    item {
                        DrebinCategoryView(
                            "S5: Restricted APIs", 
                            assessment.drebinFeatures.s5RestrictedApis, 
                            NeonCyan
                        )
                    }
                    item {
                        DrebinCategoryView(
                            "S8: Network Analysis", 
                            assessment.drebinFeatures.s8NetworkAddresses, 
                            Color.Yellow
                        )
                    }
                    item {
                        DrebinCategoryView(
                            "S3: App Components", 
                            assessment.drebinFeatures.s3AppComponents, 
                            TextSecondary
                        )
                    }
                    item {
                        DrebinCategoryView(
                            "S1: Hardware Access", 
                            assessment.drebinFeatures.s1Hardware.map { it.substringAfterLast('.') }, 
                            Color.Green
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
