package com.codekhoda.presentation.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.codekhoda.presentation.components.*
import com.codekhoda.presentation.theme.*

@Composable
fun PermissionDashboardScreen(
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val grantedCount = uiState.permissions.count { it.isGranted }
    val totalCount = uiState.permissions.size
    val securityScore = if (totalCount > 0) (grantedCount.toFloat() / totalCount.toFloat() * 100).toInt() else 100

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        // Decorative background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(NeonCyan.copy(alpha = 0.05f), DeepBlack)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SecurityScoreHeader(score = securityScore)
            }

            item {
                Text(
                    text = "DETECTION LAYERS",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            val vulnerablePermission = uiState.permissions.find { !it.isGranted }
            if (vulnerablePermission != null) {
                item {
                    SecurityNudgeBanner(text = "Enable ${vulnerablePermission.name} for full protection.")
                }
            }

            items(uiState.permissions) { permission ->
                PermissionItem(permission = permission)
            }
            
            item {
                Text(
                    text = "BEHAVIORAL SCANNERS",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            item {
                AdvancedScannerItem(
                    name = "Accessibility Watchdog",
                    description = "Monitors for invisible overlays and phishing screens.",
                    status = "Active",
                    color = SafeGreen
                )
            }
            
            item {
                AdvancedScannerItem(
                    name = "Network Fingerprinter",
                    description = "Detects C2 communication and data exfiltration patterns.",
                    status = "Active",
                    color = SafeGreen
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AdvancedScannerItem(name: String, description: String, status: String, color: Color) {
    StatusCard(
        status = CardStatus.SAFE,
        modifier = Modifier.fillMaxWidth(),
        animated = false
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = status,
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SecurityScoreHeader(score: Int) {
    val status = when {
        score >= 90 -> CardStatus.SAFE
        score >= 60 -> CardStatus.WARNING
        else -> CardStatus.DANGER
    }
    
    StatusCard(
        status = status,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        animated = score < 90
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SECURITY SCORE",
                color = TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$score%",
                color = when(status) {
                    CardStatus.SAFE -> SafeGreen
                    CardStatus.WARNING -> WarningOrange
                    else -> AlertRed
                },
                fontSize = 56.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (score >= 90) "SYSTEM FULLY PROTECTED" else "DEVICE VULNERABILITIES DETECTED",
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PermissionItem(permission: PermissionStatus) {
    val status = if (permission.isGranted) CardStatus.SAFE else CardStatus.NEUTRAL
    
    StatusCard(
        status = status,
        modifier = Modifier.fillMaxWidth(),
        animated = !permission.isGranted
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIndicator(
                        status = if (permission.isGranted) IndicatorStatus.SAFE else IndicatorStatus.DANGER,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = permission.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            if (!permission.isGranted) {
                CyberButton(
                    text = "FIX",
                    onClick = { /* Action handled by ComponentActivity in real app */ },
                    variant = ButtonVariant.DANGER,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Safe",
                    tint = SafeGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
