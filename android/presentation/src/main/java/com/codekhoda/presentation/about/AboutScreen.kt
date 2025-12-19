package com.codekhoda.presentation.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codekhoda.presentation.theme.NeonCyan
import com.codekhoda.presentation.theme.TextSecondary

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            text = "HYBRID CLOUD SENTINEL",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonCyan,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Version 1.0.0-BETA",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Project Sentinel is an advanced AI-powered security assistant designed to protect your device from emerging threats using a hybrid approach: on-device TFLite inference and cloud-based deep analysis.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Hybrid Architecture Section
        Text(
            text = "Hybrid Architecture:",
            style = MaterialTheme.typography.titleMedium,
            color = NeonCyan
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Our lightweight on-device agent combines the best of both worlds:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = TextSecondary
        )
        Spacer(Modifier.height(16.dp))
        
        BulletPoint("📱 On-Device ML: TensorFlow Lite models run locally for instant threat detection")
        BulletPoint("☁️ Cloud Analysis: Deep neural networks analyze complex patterns in the cloud")
        BulletPoint("⚡ Lightweight Agent: Minimal battery and performance impact")
        BulletPoint("🔋 Optimized: Smart resource management for all device types")
        
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Key Features:",
            style = MaterialTheme.typography.titleMedium,
            color = NeonCyan
        )
        Spacer(Modifier.height(8.dp))
        BulletPoint("Real-time App Scanning")
        BulletPoint("AI-Powered Malware Detection")
        BulletPoint("Cloud Brain Threat Analysis")
        BulletPoint("Heuristic Explainability")
        BulletPoint("Low Speed Mode for Battery Saving")
        
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Performance Metrics:",
            style = MaterialTheme.typography.titleMedium,
            color = NeonCyan
        )
        Spacer(Modifier.height(8.dp))
        MetricItem("Battery Impact", "<2% during scanning")
        MetricItem("Memory Footprint", "~15MB RAM usage")
        MetricItem("Scan Speed", "~100 apps/minute")
        MetricItem("Model Size", "4.8MB TFLite model")
        
        Spacer(Modifier.height(48.dp))
        Text(
            text = "Developed by CodeKhoda team",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = NeonCyan,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "•", color = NeonCyan, modifier = Modifier.padding(end = 8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
