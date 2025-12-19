package com.codekhoda.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.presentation.permissions.PermissionViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    permissionViewModel: PermissionViewModel = hiltViewModel(),
    onNavigateToSecurity: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val permissionState by permissionViewModel.uiState.collectAsState()
    
    val vulnerablePermission = permissionState.permissions.find { !it.isGranted }
    var selectedAssessment by remember { mutableStateOf<RiskAssessment?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        // Decorative Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = 0.15f),
                            DeepBlack
                        ),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "HYBRID SENTINEL",
                style = MaterialTheme.typography.headlineMedium,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Radar Visualization
            RadarVisualization(
                isScanning = state.isScanning,
                progress = state.progress,
                scanPhase = determineScanPhase(state),
                threatCount = state.results.count { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Encouragement Loop: Nudge if vulnerable
            if (!state.isScanning && vulnerablePermission != null) {
                SecurityNudgeBanner(
                    text = "Enable ${vulnerablePermission.name} for better protection.",
                    onClick = onNavigateToSecurity,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Action Area
            if (!state.isScanning) {
                // Low Speed Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.isLowSpeedMode,
                        onCheckedChange = { viewModel.toggleLowSpeedMode() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NeonCyan,
                            uncheckedColor = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Low Speed Scan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CyberButton(
                    text = if (state.results.isEmpty()) "INITIATE SYSTEM SCAN" else "RESCAN SYSTEM",
                    onClick = { viewModel.startScan(state.isLowSpeedMode) },
                    variant = ButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            } else {
                ScanningStatus(state.currentApp)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CyberButton(
                    text = "STOP SCAN",
                    onClick = { viewModel.stopScan() },
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Results List
            if (state.results.isNotEmpty()) {
                Text(
                    text = "SCAN RESULTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = state.results,
                        key = { it.packageName }
                    ) { result ->
                        ResultItemCard(
                            result = result,
                            onClick = { selectedAssessment = result }
                        )
                    }
                }
            }
        }

        // Details Logic
        if (selectedAssessment != null) {
            Dialog(onDismissRequest = { selectedAssessment = null }) {
                ThreatDetailsScreen(
                    assessment = selectedAssessment!!,
                    onDismiss = { selectedAssessment = null }
                )
            }
        }
    }
}

@Composable
fun ScanningStatus(currentApp: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "SCANNING PACKAGE",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currentApp.substringAfterLast('.'),
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
fun ResultItemCard(
    result: RiskAssessment,
    onClick: () -> Unit
) {
    val status = when (result.riskLevel) {
        RiskLevel.SAFE -> CardStatus.SAFE
        RiskLevel.LOW -> CardStatus.NEUTRAL
        RiskLevel.MEDIUM -> CardStatus.WARNING
        else -> CardStatus.DANGER
    }

    StatusCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        status = status,
        animated = status == CardStatus.DANGER || status == CardStatus.WARNING
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIndicator(
                status = when (result.riskLevel) {
                    RiskLevel.SAFE -> IndicatorStatus.SAFE
                    RiskLevel.LOW -> IndicatorStatus.NEUTRAL
                    RiskLevel.MEDIUM -> IndicatorStatus.WARNING
                    else -> IndicatorStatus.DANGER
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.packageName.substringAfterLast('.'),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = result.riskLevel.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.ArrowForward,
                contentDescription = "Details",
                tint = TextSecondary
            )
        }
    }
}

private fun determineScanPhase(state: ScanUiState): ScanPhase {
    return when {
        !state.isScanning && state.results.isNotEmpty() -> ScanPhase.VERDICT
        !state.isScanning -> ScanPhase.IDLE
        state.progress < 0.3f -> ScanPhase.EXTRACTING
        else -> ScanPhase.ANALYZING
    }
}
