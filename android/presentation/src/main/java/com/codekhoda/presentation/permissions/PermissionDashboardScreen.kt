package com.codekhoda.presentation.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.codekhoda.presentation.components.SecurityNudgeBanner

@Composable
fun PermissionDashboardScreen(
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val grantedCount = uiState.permissions.count { it.isGranted }
    val totalCount = uiState.permissions.size
    val securityScore = if (totalCount > 0) (grantedCount.toFloat() / totalCount.toFloat() * 100).toInt() else 100

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
                text = "Anomaly Detection Layers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        item {
            SecurityNudgeBanner()
        }

        items(uiState.permissions) { permission ->
            PermissionItem(permission = permission)
        }
        
        item {
            Text(
                text = "Advanced Behavioral Scanners",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        item {
            AdvancedScannerItem(
                name = "Accessibility Watchdog",
                description = "Monitors for invisible overlays and phishing screens.",
                status = "Active",
                color = Color(0xFF4CAF50)
            )
        }
        
        item {
            AdvancedScannerItem(
                name = "Network Fingerprinter",
                description = "Detects C2 communication and data exfiltration patterns.",
                status = "Active",
                color = Color(0xFF4CAF50)
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AdvancedScannerItem(name: String, description: String, status: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
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
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (score >= 90) Color(0xFF1B5E20) else if (score >= 60) Color(0xFFE65100) else Color(0xFFB71C1C)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Security Score",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "$score%",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (score >= 90) "System Fully Protected" else "Your device is vulnerable",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PermissionItem(permission: PermissionStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (permission.isGranted) "🟢" else "🔴",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = permission.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = permission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!permission.isGranted) {
                Button(
                    onClick = { /* Action handled by ComponentActivity in real app */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Fix")
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Warning, // Mock check icon
                    contentDescription = "Safe",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
