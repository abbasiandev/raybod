package dev.abbasian.presentation.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abbasian.presentation.components.*
import dev.abbasian.presentation.theme.*

@Composable
fun AboutScreen() {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            
            Text(
                text = "RAYBOD",
                style = MaterialTheme.typography.headlineSmall,
                color = NeonCyan,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "SYSTEM VERSION 1.0.0-FINAL",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(32.dp))
            
            StatusCard(
                status = CardStatus.NEUTRAL,
                modifier = Modifier.fillMaxWidth(),
                animated = false
            ) {
                Text(
                    text = "Project Sentinel is an advanced AI-powered security assistant designed to protect your device from emerging threats using the DREBIN methodology: on-device TFLite inference and cloud-based deep forensic analysis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                    lineHeight = 22.sp
                )
            }
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                text = "CORE TECHNOLOGIES",
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BulletPointItem("📱 ON-DEVICE ML: DREBIN-powered TFLite models run locally for instant detection")
                BulletPointItem("☁️ CLOUD FORENSICS: Deep analysis using 8 S-Set categorized AI vectors")
                BulletPointItem("🔄 OTA UPDATES: Automated background model retraining and synchronization")
                BulletPointItem("🛡️ PRIVACY MONITORING: Real-time permission and AppOps usage tracking")
                BulletPointItem("⚡ PERFORMANCE: Optimized for minimal battery and memory footprint")
            }
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                text = "PERFORMANCE METRICS",
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(Modifier.height(16.dp))
            
            StatusCard(
                status = CardStatus.SAFE,
                modifier = Modifier.fillMaxWidth(),
                animated = false
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MetricItem("AI Methodology", "DREBIN V2")
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = SafeGreen.copy(alpha = 0.1f))
                    MetricItem("Forensic Depth", "8 S-SETS")
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = SafeGreen.copy(alpha = 0.1f))
                    MetricItem("Battery Impact", "< 1.5%")
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = SafeGreen.copy(alpha = 0.1f))
                    MetricItem("Scan Latency", "~ 450ms/app")
                }
            }
            
            Spacer(Modifier.height(48.dp))
            
            Text(
                text = "DEVELOPED BY CODEKHODA TEAM",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = SafeGreen,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun BulletPointItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "▶", color = NeonCyan, fontSize = 10.sp, modifier = Modifier.padding(end = 12.dp, top = 4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            lineHeight = 20.sp
        )
    }
}
