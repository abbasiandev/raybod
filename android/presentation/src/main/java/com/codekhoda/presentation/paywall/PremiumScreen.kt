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
import androidx.hilt.navigation.compose.hiltViewModel
import com.codekhoda.presentation.components.*
import com.codekhoda.presentation.theme.*

@Composable
fun PremiumScreen(
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showPaymentDialog by remember { mutableStateOf(false) }
    val userPlan by viewModel.userPlan.collectAsState()
    
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Payment Sandbox", color = TextPrimary) },
            text = { Text("Select your payment status to continue. This is a sandbox simulation for the 2025 version.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePlan(true)
                        showPaymentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                ) {
                    Text("Paid", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.updatePlan(false)
                        showPaymentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Not Paid", color = Color.White)
                }
            },
            containerColor = CardSurface,
            textContentColor = TextPrimary
        )
    }

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
            "SELECT YOUR PLAN",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonPink,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            "Current Plan: $userPlan",
            style = MaterialTheme.typography.bodyMedium,
            color = if (userPlan == "PREMIUM") SafeGreen else TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Freemium Card
        StatusCard(
            status = CardStatus.SAFE,
            modifier = Modifier.fillMaxWidth(),
            animated = false
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "FREEMIUM",
                    style = MaterialTheme.typography.labelLarge,
                    color = SafeGreen,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    "$0/mo",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    PremiumFeatureItem(
                        title = "On-Device ML Scanning",
                        description = "Essential local protection",
                        isFree = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumFeatureItem(
                        title = "Basic Threat Reports",
                        description = "Standard detection logs",
                        isFree = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumFeatureItem(
                        title = "Manual Scans (3 Daily)",
                        description = "Basic on-demand security",
                        isFree = true
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                CyberButton(
                    text = "Start for Free",
                    onClick = { /* Already on free plan usually */ },
                    variant = ButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Featured/Premium Card
        StatusCard(
            status = CardStatus.SCANNING,
            modifier = Modifier.fillMaxWidth(),
            animated = true
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "FEATURED",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonPurple,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    "$4.99/mo",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    PremiumFeatureItem(
                        title = "Real-time Cloud Analysis",
                        description = "Powerful infrastructure protection",
                        isFree = false
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumFeatureItem(
                        title = "Unlimited Background Scans",
                        description = "Continuous monitoring",
                        isFree = false
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumFeatureItem(
                        title = "Zero-Day Protection",
                        description = "AI detection for new threats",
                        isFree = false
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                CyberButton(
                    text = "Upgrade Now",
                    onClick = { showPaymentDialog = true },
                    variant = ButtonVariant.GRADIENT,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "© 2025 CodeKhoda Security Lab",
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
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isFree) NeonCyan else NeonPink,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 14.sp
            )
        }
    }
}
