package com.codekhoda.presentation.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

