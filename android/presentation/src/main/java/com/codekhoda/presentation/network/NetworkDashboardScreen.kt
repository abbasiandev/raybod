package com.codekhoda.presentation.network

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.codekhoda.domain.model.NetworkAlert
import com.codekhoda.domain.model.NetworkFlow
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.presentation.components.*
import com.codekhoda.presentation.theme.*

@Composable
fun NetworkDashboardScreen(
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val activeFlows by viewModel.activeFlows.collectAsState()
    val alerts by viewModel.networkAlerts.collectAsState()
    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val isSimulating by viewModel.isSimulating.collectAsState()
    val context = LocalContext.current

    // Track alert count to trigger flash animation
    var lastAlertCount by remember { mutableIntStateOf(alerts.size) }
    var showThreatFlash by remember { mutableStateOf(false) }

    LaunchedEffect(alerts.size) {
        if (alerts.size > lastAlertCount) {
            showThreatFlash = true
            kotlinx.coroutines.delay(800)
            showThreatFlash = false
        }
        lastAlertCount = alerts.size
    }

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            startVpnService(context)
            viewModel.setVpnActive(true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        // Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.1f),
                            DeepBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Shield Control Card
            ShieldControlCard(
                isActive = isVpnActive,
                onToggle = { active ->
                    if (active) {
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            vpnPrepareLauncher.launch(intent)
                        } else {
                            startVpnService(context)
                            viewModel.setVpnActive(true)
                        }
                    } else {
                        stopVpnService(context)
                        viewModel.setVpnActive(false)
                    }
                }
            )

            if (isVpnActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.toggleSimulation() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulating) NeonPink.copy(alpha = 0.4f) else NeonPink.copy(alpha = 0.2f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        if (isSimulating) "STOP SIMULATION" else "SIMULATE THREAT DETECTION",
                        color = NeonPink,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Active Connections",
                    value = activeFlows.size.toString(),
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Threats Neutralized",
                    value = alerts.size.toString(),
                    color = NeonPink,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Traffic Pulse Visualization (Decorative)
            TrafficPulseBox(
                isVpnActive && activeFlows.isNotEmpty(),
                showThreatFlash,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section Tabs
            var selectedTab by remember { mutableStateOf(0) }
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = NeonCyan,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("LIVE TRAFFIC") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("THREAT LOG") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                LiveTrafficList(activeFlows)
            } else {
                ThreatLogList(alerts)
            }
        }
    }
}

@Composable
fun ShieldControlCard(isActive: Boolean, onToggle: (Boolean) -> Unit) {
    GlowingCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = if (isActive) NeonCyan else TextSecondary,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isActive) NeonCyan.copy(alpha = 0.1f) else TextSecondary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Lock else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isActive) NeonCyan else TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isActive) "Network Shield Active" else "Network Shield Idle",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = if (isActive) "Inspecting all packets in real-time" else "VPN disabled - no traffic analysis",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = TextSecondary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

private fun startVpnService(context: android.content.Context) {
    val intent = Intent(context, Class.forName("com.codekhoda.agent.service.SentinelVpnService"))
    context.startService(intent)
}

private fun stopVpnService(context: android.content.Context) {
    val intent = Intent(context, Class.forName("com.codekhoda.agent.service.SentinelVpnService"))
    context.stopService(intent)
}

@Composable
fun TrafficPulseBox(isActive: Boolean, isThreatDetected: Boolean, modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "TrafficPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "PulseProgress"
    )

    val threatColor = animateColorAsState(
        targetValue = if (isThreatDetected) NeonPink else NeonCyan,
        animationSpec = tween(300),
        label = "ThreatColor"
    )
    
    val threatAlpha = animateFloatAsState(
        targetValue = if (isThreatDetected) 0.3f else 0.05f,
        animationSpec = tween(300),
        label = "ThreatAlpha"
    )

    // Generate random properties for each line once
    val lineProperties = remember {
        val random = java.util.Random()
        List(6) { 
            Triple(
                0.2f + random.nextFloat() * 0.8f, // speed multiplier (0.2 to 1.0)
                random.nextFloat(),               // initial offset (0.0 to 1.0)
                (2 + random.nextInt(4)).dp        // size (2dp to 5dp)
            )
        }
    }

    StatusCard(
        status = when {
            isThreatDetected -> CardStatus.DANGER
            isActive -> CardStatus.SCANNING
            else -> CardStatus.NEUTRAL
        },
        modifier = modifier,
        animated = isActive || isThreatDetected
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Draw background grid
            val gridStep = 40.dp.toPx()
            for (x in 0 until (width / gridStep).toInt() + 1) {
                drawLine(
                    color = threatColor.value.copy(alpha = 0.03f),
                    start = Offset(x * gridStep, 0f),
                    end = Offset(x * gridStep, height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }

            // Draw horizontal data lines
            val lineCount = lineProperties.size
            lineProperties.forEachIndexed { i, props ->
                val speedMult = props.first
                val initialOffset = props.second
                val dotSize = props.third.toPx()
                
                val y = height * (i + 1) / (lineCount + 1)
                
                drawLine(
                    color = threatColor.value.copy(alpha = threatAlpha.value),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Animated pulse on each line
                if (isActive) {
                    // Use line-specific speed and offset
                    val individualProgress = (pulseProgress * speedMult + initialOffset) % 1f
                    val pulseX = width * individualProgress
                    
                    drawCircle(
                        color = threatColor.value.copy(alpha = 0.6f),
                        radius = dotSize,
                        center = Offset(pulseX, y)
                    )
                    drawCircle(
                        color = threatColor.value.copy(alpha = 0.15f),
                        radius = dotSize * 2.5f,
                        center = Offset(pulseX, y)
                    )
                    
                    // Add secondary smaller packet on some lines
                    if (i % 2 == 0) {
                        val secondaryProgress = (individualProgress + 0.3f) % 1f
                        val secondaryX = width * secondaryProgress
                        drawCircle(
                            color = threatColor.value.copy(alpha = 0.4f),
                            radius = dotSize * 0.7f,
                            center = Offset(secondaryX, y)
                        )
                    }
                }
            }

            // Scanner Beam Effect
            if (isActive) {
                val scannerX = width * pulseProgress
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            threatColor.value.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        startX = scannerX - 40.dp.toPx(),
                        endX = scannerX + 40.dp.toPx()
                    ),
                    topLeft = Offset(scannerX - 40.dp.toPx(), 0f),
                    size = androidx.compose.ui.geometry.Size(80.dp.toPx(), height)
                )
                
                drawLine(
                    color = threatColor.value.copy(alpha = 0.4f),
                    start = Offset(scannerX, 0f),
                    end = Offset(scannerX, height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when {
                        isThreatDetected -> "THREAT DETECTED!"
                        isActive -> "ENCRYPTED TUNNEL ACTIVE"
                        else -> "SHIELD STANDBY"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isThreatDetected -> NeonPink
                        isActive -> NeonCyan
                        else -> TextSecondary
                    },
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                if (isActive || isThreatDetected) {
                    Text(
                        text = if (isThreatDetected) "MALICIOUS PACKET INTERCEPTED" else "REAL-TIME PACKET INSPECTION",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isThreatDetected) NeonPink.copy(alpha = 0.7f) else TextPrimary.copy(alpha = 0.7f),
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    GlowingCard(
        modifier = modifier,
        glowColor = color,
        cornerRadius = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LiveTrafficList(flows: List<NetworkFlow>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (flows.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Monitoring Network Traffic...", color = TextSecondary)
                }
            }
        }
        items(flows) { flow ->
            StatusCard(
                status = if (flow.riskLevel == RiskLevel.SAFE) CardStatus.SAFE else CardStatus.NEUTRAL,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = flow.domain ?: flow.destinationIp, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        Text(text = "${flow.protocol} • ${flow.sourceApp}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Text(text = "${flow.bytesSent / 1024} KB", style = MaterialTheme.typography.bodyMedium, color = NeonCyan)
                }
            }
        }
    }
}

@Composable
fun ThreatLogList(alerts: List<NetworkAlert>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (alerts.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Network Threats Detected", color = SafeGreen)
                }
            }
        }
        items(alerts) { alert ->
            StatusCard(
                status = CardStatus.DANGER,
                modifier = Modifier.fillMaxWidth(),
                animated = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusIndicator(status = IndicatorStatus.DANGER)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = alert.threatType,
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonPink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "App: ${alert.packageName} • Dest: ${alert.destination}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

