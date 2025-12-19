package com.codekhoda.presentation.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.presentation.components.*
import com.codekhoda.presentation.theme.*
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.drawable.toBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatDetailsScreen(
    packageName: String,
    viewModel: ScanViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val assessment = state.results.find { it.packageName == packageName }
    val context = LocalContext.current

    val appIcon = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    if (assessment == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Assessment not found", color = TextPrimary)
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "FORENSIC ANALYSIS",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepBlack,
                    titleContentColor = NeonCyan
                )
            )
        },
        containerColor = DeepBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(DeepBlack, DarkSurface)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Main Status Header
                StatusCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    status = when (assessment.riskLevel) {
                        RiskLevel.SAFE -> CardStatus.SAFE
                        RiskLevel.LOW -> CardStatus.NEUTRAL
                        RiskLevel.MEDIUM -> CardStatus.WARNING
                        else -> CardStatus.DANGER
                    },
                    animated = assessment.riskLevel != RiskLevel.SAFE
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    getRiskColor(assessment.riskLevel).copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (appIcon != null) {
                                Image(
                                    bitmap = appIcon,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = getRiskIcon(assessment.riskLevel),
                                    contentDescription = null,
                                    tint = getRiskColor(assessment.riskLevel),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = assessment.packageName.split('.').last().uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = assessment.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(
                                    containerColor = getRiskColor(assessment.riskLevel).copy(alpha = 0.2f),
                                    contentColor = getRiskColor(assessment.riskLevel)
                                ) {
                                    Text(
                                        text = assessment.riskLevel.name,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (assessment.riskLevel == RiskLevel.SAFE) "SYSTEM CLEAN" else "THREAT DETECTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (assessment.riskLevel == RiskLevel.SAFE) SafeGreen else NeonPink
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Detection Log
                    item {
                        SectionHeader(title = "DETECTION LOG", icon = Icons.Default.List)
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = assessment.description.ifEmpty { "Static and behavioral analysis confirms no known malicious vectors. The package signature matches trusted repository standards." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary.copy(alpha = 0.8f),
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }

                    // Forensics Sections
                    item { SectionHeader(title = "DREBIN AI VECTORS", icon = Icons.Default.Search) }
                    
                    item {
                        DrebinCategoryView(
                            "S2: REQUESTED PERMISSIONS", 
                            assessment.drebinFeatures.s2RequestedPermissions.filter { it.contains("android.permission.") }
                                .map { it.substringAfterLast('.') },
                            NeonCyan
                        )
                    }
                    
                    item {
                        DrebinCategoryView(
                            "S7: SUSPICIOUS PATTERNS", 
                            assessment.drebinFeatures.s7SuspiciousApis, 
                            NeonPink
                        )
                    }
                    
                    item {
                        DrebinCategoryView(
                            "S5: RESTRICTED APIS", 
                            assessment.drebinFeatures.s5RestrictedApis, 
                            NeonPurple
                        )
                    }
                    
                    item {
                        DrebinCategoryView(
                            "S8: NETWORK INFRASTRUCTURE", 
                            assessment.drebinFeatures.s8NetworkAddresses, 
                            WarningOrange
                        )
                    }
                }

                // Action Footer
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CyberButton(
                            text = "BACK",
                            onClick = onNavigateBack,
                            variant = ButtonVariant.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (assessment.riskLevel != RiskLevel.SAFE) {
                            CyberButton(
                                text = "PURGE THREAT",
                                onClick = { 
                                    val intent = Intent(Intent.ACTION_DELETE).apply {
                                        data = Uri.parse("package:${assessment.packageName}")
                                    }
                                    context.startActivity(intent)
                                },
                                variant = ButtonVariant.DANGER,
                                modifier = Modifier.weight(1.5f),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = NeonCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun getRiskColor(riskLevel: RiskLevel): Color {
    return when (riskLevel) {
        RiskLevel.SAFE -> SafeGreen
        RiskLevel.LOW -> NeonCyan
        RiskLevel.MEDIUM -> WarningOrange
        RiskLevel.HIGH -> NeonPink
        RiskLevel.CRITICAL -> CriticalMagenta
        RiskLevel.UNKNOWN -> TextSecondary
    }
}

@Composable
private fun getRiskIcon(riskLevel: RiskLevel): ImageVector {
    return when (riskLevel) {
        RiskLevel.SAFE -> Icons.Default.CheckCircle
        RiskLevel.LOW -> Icons.Default.Info
        RiskLevel.MEDIUM -> Icons.Default.Warning
        RiskLevel.HIGH -> Icons.Default.Security
        RiskLevel.CRITICAL -> Icons.Default.Dangerous
        RiskLevel.UNKNOWN -> Icons.Default.Help
    }
}

@Composable
fun DrebinCategoryView(title: String, features: List<String>, color: Color) {
    if (features.isEmpty()) return
    
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        features.forEach { feature ->
            VectorItem(feature, color)
        }
    }
}

@Composable
fun VectorItem(text: String, color: Color) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        cornerRadius = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
