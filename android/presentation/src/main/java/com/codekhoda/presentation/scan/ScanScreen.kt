package com.codekhoda.presentation.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.presentation.theme.NeonCyan
import com.codekhoda.presentation.theme.AlertRed

@Composable
fun ScanScreen(
    viewModel: ScanViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Radar Visualization
        RadarView(isScanning = state.isScanning)

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isScanning) {
            Text("Scanning: ${state.currentApp}", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = state.progress,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = NeonCyan
            )
        } else {
            Button(
                onClick = { viewModel.startScan() },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("INITIATE CLOUD SCAN", color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results List
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(state.results) { result ->
                ResultItem(result)
            }
        }
    }
}

@Composable
fun ResultItem(result: com.codekhoda.domain.model.RiskAssessment) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.packageName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = result.riskLevel.name,
                    color = if (result.riskLevel == RiskLevel.SAFE) Color.Green else AlertRed,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (result.description.isNotEmpty()) {
                Text(
                    text = result.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun RadarView(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "Rotation"
    )

    Canvas(modifier = Modifier.size(200.dp)) {
        drawCircle(
            color = NeonCyan.copy(alpha = 0.3f),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = NeonCyan.copy(alpha = 0.1f),
            radius = size.minDimension / 4
        )
        
        if (isScanning) {
            drawArc(
                color = NeonCyan,
                startAngle = rotation,
                sweepAngle = 90f,
                useCenter = true,
                alpha = 0.5f
            )
        }
    }
}
