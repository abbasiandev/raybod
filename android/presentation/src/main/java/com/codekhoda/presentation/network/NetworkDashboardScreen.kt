package com.codekhoda.presentation.network

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    val context = LocalContext.current

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
                    onClick = { viewModel.simulateMaliciousFlow() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink.copy(alpha = 0.2f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("SIMULATE THREAT DETECTION", color = NeonPink, style = MaterialTheme.typography.labelLarge)
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
                isActive = isVpnActive && activeFlows.isNotEmpty(),
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
fun TrafficPulseBox(isActive: Boolean, modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "TrafficPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "PulseProgress"
    )

    StatusCard(
        status = if (isActive) CardStatus.SCANNING else CardStatus.NEUTRAL,
        modifier = modifier,
        animated = isActive
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2, height / 2)

            // Draw horizontal data lines
            val lineCount = 5
            for (i in 0 until lineCount) {
                val y = height * (i + 1) / (lineCount + 1)
                drawLine(
                    color = NeonCyan.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Animated pulse on each line
                if (isActive) {
                    val pulseX = (width * ((pulseProgress + (i * 0.2f)) % 1f))
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.6f),
                        radius = 3.dp.toPx(),
                        center = Offset(pulseX, y)
                    )
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.2f),
                        radius = 8.dp.toPx(),
                        center = Offset(pulseX, y)
                    )
                }
            }

            // Central Status Text
            val statusText = if (isActive) "ENCRYPTED TUNNEL ACTIVE" else "IDLE"
            // We can't easily draw text in Canvas without native canvas, so we'll use a Box overlay instead
        }
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isActive) "ENCRYPTED TUNNEL ACTIVE" else "SHIELD STANDBY",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) NeonCyan else TextSecondary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                if (isActive) {
                    Text(
                        text = "REAL-TIME PACKET INSPECTION",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary.copy(alpha = 0.7f),
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
                status = if (flow.riskLevel == RiskLevel.SAFE) CardStatus.SAFE else CardStatus.NEUTRAL
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
                animated = true
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusIndicator(status = IndicatorStatus.DANGER)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = alert.threatType, style = MaterialTheme.typography.titleMedium, color = NeonPink)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = alert.description, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "App: ${alert.packageName} • Dest: ${alert.destination}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}

