package com.codekhoda.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.presentation.components.*
import com.codekhoda.presentation.theme.*

@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    onNavigateToPremium: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    var selectedAssessment by remember { mutableStateOf<RiskAssessment?>(null) }
    var showLowSpeedInfo by remember { mutableStateOf(false) }
    var showUpgradePrompt by remember { mutableStateOf(false) }

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

            // Action Area
            if (!state.isScanning) {
                // Low Speed Mode Toggle with Info Icon
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
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showLowSpeedInfo = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Low Speed Info",
                            tint = NeonCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
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

        // Low Speed Info Dialog
        if (showLowSpeedInfo) {
            Dialog(onDismissRequest = { showLowSpeedInfo = false }) {
                LowSpeedInfoDialog(onDismiss = { showLowSpeedInfo = false })
            }
        }
        
        // Upgrade Prompt Dialog
        if (showUpgradePrompt) {
            Dialog(onDismissRequest = { showUpgradePrompt = false }) {
                UpgradePromptDialog(
                    onDismiss = { showUpgradePrompt = false },
                    onUpgrade = {
                        showUpgradePrompt = false
                        onNavigateToPremium?.invoke()
                    }
                )
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
    
    // Show upgrade prompt after scan completes (first time only)
    LaunchedEffect(state.isScanning, state.results) {
        if (!state.isScanning && state.results.isNotEmpty() && onNavigateToPremium != null) {
            // Show after 2 seconds delay
            kotlinx.coroutines.delay(2000)
            showUpgradePrompt = true
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

@Composable
fun LowSpeedInfoDialog(onDismiss: () -> Unit) {
    GlowingCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        glowColor = NeonCyan,
        cornerRadius = 16.dp,
        animated = true
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "LOW SPEED SCAN",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = NeonCyan.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "What is Low Speed Scan?",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Low Speed Scan reduces the scanning intensity by adding small delays between each app analysis. This helps:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            BulletPointItem("Minimize battery consumption during scanning")
            BulletPointItem("Reduce CPU load for better device performance")
            BulletPointItem("Allow you to use your device normally while scanning")
            BulletPointItem("Prevent overheating on older devices")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "⚡ Our lightweight hybrid architecture ensures minimal impact even in normal speed mode!",
                style = MaterialTheme.typography.bodySmall,
                color = SafeGreen,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            CyberButton(
                text = "GOT IT",
                onClick = onDismiss,
                variant = ButtonVariant.PRIMARY,
                glowColor = NeonCyan,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun UpgradePromptDialog(onDismiss: () -> Unit, onUpgrade: () -> Unit) {
    GlowingCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        glowColor = NeonPink,
        cornerRadius = 16.dp,
        animated = true
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = NeonPink,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "UPGRADE TO PREMIUM",
                style = MaterialTheme.typography.headlineSmall,
                color = NeonPink,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Get more from your security scans",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            UpgradeFeatureItem("🚀 Real-time Cloud Analysis")
            UpgradeFeatureItem("🛡️ Zero-Day Threat Protection")
            UpgradeFeatureItem("⚡ Priority Scanning Queue")
            UpgradeFeatureItem("♾️ Unlimited Scans Daily")
            UpgradeFeatureItem("🤖 Automatic Background Scanning")
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Start with a 7-day FREE trial",
                style = MaterialTheme.typography.titleMedium,
                color = SafeGreen,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CyberButton(
                text = "TRY PREMIUM FREE",
                onClick = onUpgrade,
                variant = ButtonVariant.GRADIENT,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CyberButton(
                text = "MAYBE LATER",
                onClick = onDismiss,
                variant = ButtonVariant.SECONDARY,
                glowColor = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BulletPointItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = NeonCyan,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun UpgradeFeatureItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        textAlign = TextAlign.Start
    )
}
