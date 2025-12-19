package com.codekhoda.presentation.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codekhoda.presentation.components.ButtonVariant
import com.codekhoda.presentation.components.CyberButton
import com.codekhoda.presentation.components.GlowingCard
import com.codekhoda.presentation.theme.*

@Composable
fun PremiumScreen() {
    val context = LocalContext.current
    val billingUrl = "http://10.0.2.2:8000/dashboard/billing/" // Emulator access to localhost
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepBlack, Color(0xFF102027))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Header
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Premium Shield",
            tint = NeonPink,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "UPGRADE TO PREMIUM",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonPink,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            "Advanced Protection for Power Users",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Feature Comparison Table
        GlowingCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = NeonPink,
            cornerRadius = 16.dp,
            animated = true
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "PREMIUM FEATURES",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonPink,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PremiumFeatureItem(
                    title = "Real-time Cloud Analysis",
                    description = "Instant threat detection with our powerful cloud infrastructure",
                    isFree = false
                )
                
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = TextSecondary.copy(alpha = 0.2f)
                )
                
                PremiumFeatureItem(
                    title = "Zero-Day Threat Protection",
                    description = "Advanced AI detection for emerging threats not yet in databases",
                    isFree = false
                )
                
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = TextSecondary.copy(alpha = 0.2f)
                )
                
                PremiumFeatureItem(
                    title = "Priority Scanning Queue",
                    description = "Skip the line with dedicated server resources",
                    isFree = false
                )
                
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = TextSecondary.copy(alpha = 0.2f)
                )
                
                PremiumFeatureItem(
                    title = "Unlimited Scans",
                    description = "Scan as many apps as you want, anytime",
                    isFree = false
                )
                
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = TextSecondary.copy(alpha = 0.2f)
                )
                
                PremiumFeatureItem(
                    title = "Automatic Background Scanning",
                    description = "Continuous protection without manual intervention",
                    isFree = false
                )
                
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = TextSecondary.copy(alpha = 0.2f)
                )
                
                PremiumFeatureItem(
                    title = "Ad-Free Experience",
                    description = "Enjoy uninterrupted security monitoring",
                    isFree = false
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Free Features Card
        GlowingCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = NeonCyan,
            cornerRadius = 16.dp,
            animated = false
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "FREE FEATURES",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PremiumFeatureItem(
                    title = "On-Device ML Scanning",
                    description = "Lightweight local threat detection with TensorFlow Lite",
                    isFree = true
                )
                
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = TextSecondary.copy(alpha = 0.2f)
                )
                
                PremiumFeatureItem(
                    title = "Manual Scans (Once Daily)",
                    description = "Run a full system scan once every 24 hours",
                    isFree = true
                )
                
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = TextSecondary.copy(alpha = 0.2f)
                )
                
                PremiumFeatureItem(
                    title = "Basic Threat Reports",
                    description = "View detected threats and risk assessments",
                    isFree = true
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Pricing
        Text(
            "$4.99/month",
            style = MaterialTheme.typography.displayMedium,
            color = NeonPink,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            "Cancel anytime • 7-day free trial",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // CTA Button
        CyberButton(
            text = "START FREE TRIAL",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(billingUrl))
                context.startActivity(intent)
            },
            variant = ButtonVariant.GRADIENT,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Secure payment via Google Play",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PremiumFeatureItem(
    title: String,
    description: String,
    isFree: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (isFree) Icons.Default.CheckCircle else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isFree) NeonCyan else NeonPink,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
