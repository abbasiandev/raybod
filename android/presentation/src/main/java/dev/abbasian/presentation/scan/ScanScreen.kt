package dev.abbasian.presentation.scan

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.core.graphics.drawable.toBitmap
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import dev.abbasian.domain.model.RiskAssessment
import dev.abbasian.domain.model.RiskLevel
import dev.abbasian.presentation.permissions.PermissionViewModel
import dev.abbasian.presentation.components.*
import dev.abbasian.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    permissionViewModel: PermissionViewModel = hiltViewModel(),
    userPlan: String = "FREEMIUM",
    onNavigateToSecurity: () -> Unit,
    onNavigateToPremium: (() -> Unit)? = null,
    onNavigateToThreatDetails: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val permissionState by permissionViewModel.uiState.collectAsState()
    
    val vulnerablePermission = permissionState.permissions.find { !it.isGranted }
    var showLowSpeedInfo by remember { mutableStateOf(false) }
    var showUpgradePrompt by remember { mutableStateOf(false) }
    var showRootInfo by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showFinishAnimation by remember { mutableStateOf(false) }

    // Trigger finish animation
    LaunchedEffect(state.isScanning) {
        if (!state.isScanning && state.results.isNotEmpty()) {
            showFinishAnimation = true
            kotlinx.coroutines.delay(3000)
            showFinishAnimation = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        bottomBar = {
            if (state.isScanning) {
                Surface(
                    color = DeepBlack.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ScanningQueue(
                            recentApps = state.recentApps,
                            results = state.results
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        CyberButton(
                            text = "STOP SCAN",
                            onClick = { viewModel.stopScan() },
                            variant = ButtonVariant.SECONDARY,
                            modifier = Modifier.fillMaxWidth(0.85f),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DeepBlack)
        ) {
            // Decorative Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonPurple.copy(alpha = 0.1f),
                                DeepBlack
                            ),
                            radius = 800f
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                item {
                    // Radar Visualization - More compact
                    RadarVisualization(
                        modifier = Modifier.size(220.dp),
                        isScanning = state.isScanning,
                        progress = state.progress,
                        scanPhase = determineScanPhase(state),
                        threatCount = state.results.count { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    // Action Area
                    if (!state.isScanning) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Low Speed Mode Toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = state.isLowSpeedMode,
                                    onCheckedChange = { viewModel.toggleLowSpeedMode() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonCyan,
                                        checkedTrackColor = NeonCyan.copy(alpha = 0.5f),
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = DarkSurface
                                    ),
                                    modifier = Modifier.scale(0.7f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Low Speed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                IconButton(
                                    onClick = { showLowSpeedInfo = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = NeonCyan.copy(alpha = 0.6f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            CyberButton(
                                text = if (state.results.isEmpty()) "INITIATE SYSTEM SCAN" else "RESCAN SYSTEM",
                                onClick = { viewModel.startScan(state.isLowSpeedMode) },
                                variant = ButtonVariant.PRIMARY,
                                modifier = Modifier.fillMaxWidth(0.85f),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            )
                            
                            if (state.results.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                CyberButton(
                                    text = "VIEW SCAN RESULTS",
                                    onClick = { viewModel.setShowResultsSheet(true) },
                                    variant = ButtonVariant.GRADIENT,
                                    modifier = Modifier.fillMaxWidth(0.85f),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                        }
                    } else {
                        ScanningStatus(
                            currentApp = state.currentApp,
                            currentAppLabel = state.currentAppLabel,
                            scannedApps = state.scannedApps,
                            totalApps = state.totalApps
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // Integrity Nudges
                if (!state.isScanning) {
                    if (state.isRooted) {
                        item {
                            SecurityNudgeBanner(
                                text = "Device is ROOTED. Integrity compromised.",
                                onClick = { showRootInfo = true },
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                    if (vulnerablePermission != null) {
                        item {
                            SecurityNudgeBanner(
                                text = "Enable ${vulnerablePermission.name} for protection.",
                                onClick = onNavigateToSecurity,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }
            }

            // Results Bottom Sheet
            if (state.showResultsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.setShowResultsSheet(false) },
                    sheetState = sheetState,
                    containerColor = DarkSurface,
                    scrimColor = Color.Black.copy(alpha = 0.7f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SCAN RESULTS",
                                style = MaterialTheme.typography.titleLarge,
                                color = NeonCyan,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                            IconButton(onClick = { viewModel.setShowResultsSheet(false) }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = state.results,
                                key = { it.packageName }
                            ) { result ->
                                ResultItemCard(
                                    result = result,
                                    onClick = { 
                                        onNavigateToThreatDetails(result.packageName)
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
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

            if (showRootInfo) {
                Dialog(onDismissRequest = { showRootInfo = false }) {
                    RootInfoDialog(onDismiss = { showRootInfo = false })
                }
            }

            // Finish Animation Overlay
            AnimatedVisibility(
                visible = showFinishAnimation,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 1.2f),
                modifier = Modifier.fillMaxSize()
            ) {
                ScanCompleteAnimation(
                    threatCount = state.results.count { it.riskLevel != RiskLevel.SAFE && it.riskLevel != RiskLevel.UNKNOWN }
                )
            }
        }
    }
    
    // Show error snackbar if exists
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    // Show upgrade prompt after scan completes (first time only)
    LaunchedEffect(state.isScanning, state.results) {
        if (!state.isScanning && state.results.isNotEmpty() && onNavigateToPremium != null && userPlan != "PREMIUM") {
            // Show after 2 seconds delay
            kotlinx.coroutines.delay(2000)
            showUpgradePrompt = true
        }
    }
}

@Composable
fun ScanningStatus(
    currentApp: String,
    currentAppLabel: String,
    scannedApps: Int,
    totalApps: Int
) {
    val context = LocalContext.current
    val appIcon = remember(currentApp) {
        try {
            if (currentApp.isNotEmpty() && currentApp != "Scan Stopped" && currentApp != "Scan Complete") {
                context.packageManager.getApplicationIcon(currentApp).toBitmap().asImageBitmap()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(DarkSurface)
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonCyan.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "SCANNING PACKAGE",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = currentAppLabel,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = currentApp,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$scannedApps",
                style = MaterialTheme.typography.bodyMedium,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " / $totalApps APPS",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ScanningQueue(
    recentApps: List<Pair<String, String>>,
    results: List<RiskAssessment>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "PROCESS QUEUE",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        recentApps.reversed().forEachIndexed { index, app ->
            val alpha = 1f - (index * 0.2f)
            val assessment = results.find { it.packageName == app.first }
            val isMalicious = assessment != null && assessment.riskLevel != RiskLevel.SAFE && assessment.riskLevel != RiskLevel.UNKNOWN
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .alpha(alpha.coerceAtLeast(0.2f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            if (index == 0) NeonCyan 
                            else if (isMalicious) AlertRed 
                            else SafeGreen, 
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = app.second,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (index == 0) TextPrimary 
                            else if (isMalicious) AlertRed.copy(alpha = 0.9f) 
                            else TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (index == 0) {
                    Text(
                        text = "ANALYZING",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = if (isMalicious) "THREAT" else "CLEAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMalicious) AlertRed else SafeGreen.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
                imageVector = Icons.Default.ArrowForward,
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

@Composable
fun RootInfoDialog(onDismiss: () -> Unit) {
    GlowingCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        glowColor = AlertRed,
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
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AlertRed,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "ROOT ACCESS DETECTED",
                    style = MaterialTheme.typography.titleLarge,
                    color = AlertRed,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "What does this mean?",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your device has been rooted, which means the Android security sandbox has been modified. This allows apps to access system files and data of other apps.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            BulletPointItem("Security protections can be bypassed by malware")
            BulletPointItem("Sensitive data (passwords, banking) is at higher risk")
            BulletPointItem("System stability may be compromised")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "⚠️ Recommendation: Use a non-rooted device for maximum security with Sentinel.",
                style = MaterialTheme.typography.bodySmall,
                color = WarningOrange,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            CyberButton(
                text = "UNDERSTOOD",
                onClick = onDismiss,
                variant = ButtonVariant.DANGER,
                glowColor = AlertRed,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ScanCompleteAnimation(threatCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "finish")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        // Background Glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            (if (threatCount > 0) AlertRed else SafeGreen).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Rotating Hexagon or Circle
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .size(150.dp)
                        .scale(scale)
                        .rotate(rotation),
                    tint = if (threatCount > 0) AlertRed else SafeGreen
                )
                
                // Success/Alert Icon
                Icon(
                    imageVector = if (threatCount > 0) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (threatCount > 0) "ANALYSIS COMPLETE" else "SYSTEM SECURE",
                style = MaterialTheme.typography.headlineMedium,
                color = if (threatCount > 0) AlertRed else SafeGreen,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (threatCount > 0) "$threatCount POTENTIAL THREATS DETECTED" else "NO THREATS FOUND",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "PROTECTION ACTIVE",
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan,
                modifier = Modifier
                    .border(1.dp, NeonCyan, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
