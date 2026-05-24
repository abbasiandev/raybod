package dev.abbasian.presentation.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abbasian.presentation.components.ButtonVariant
import dev.abbasian.presentation.components.CyberButton
import dev.abbasian.presentation.components.GlowingCard
import dev.abbasian.presentation.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepBlack, Color(0xFF0A0F1A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Page Indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (pagerState.currentPage == index) 32.dp else 8.dp, 8.dp)
                            .background(
                                color = if (pagerState.currentPage == index) NeonCyan else TextMuted,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> OnboardingPage1()
                    1 -> OnboardingPage2()
                    2 -> OnboardingPage3()
                    3 -> OnboardingPagePermissions()
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation Buttons
            if (pagerState.currentPage < 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CyberButton(
                        text = "SKIP",
                        onClick = onComplete,
                        variant = ButtonVariant.SECONDARY,
                        glowColor = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    CyberButton(
                        text = "NEXT",
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                CyberButton(
                    text = "GET STARTED",
                    onClick = onComplete,
                    variant = ButtonVariant.GRADIENT,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingPage1() {
    OnboardingPageContent(
        icon = Icons.Default.CheckCircle,
        iconColor = NeonCyan,
        title = "RAYBOD",
        subtitle = "Advanced Mobile Security",
        description = "Next-generation threat detection combining the power of cloud AI with lightweight on-device analysis.",
        features = listOf(
            "🛡️ Real-time malware detection",
            "🤖 AI-powered threat analysis",
            "⚡ Lightning-fast scanning",
            "🔒 Privacy-first approach"
        )
    )
}

@Composable
private fun OnboardingPage2() {
    OnboardingPageContent(
        icon = Icons.Default.Phone,
        iconColor = SafeGreen,
        title = "LIGHTWEIGHT & EFFICIENT",
        subtitle = "Designed for Performance",
        description = "Our hybrid architecture uses a lightweight on-device agent that runs ML models locally, with optional cloud backup for deep analysis.",
        features = listOf(
            "📱 Minimal battery consumption",
            "💾 Low memory footprint",
            "🚀 No device slowdown",
            "🔋 Optimized for all devices"
        )
    )
}

@Composable
private fun OnboardingPage3() {
    OnboardingPageContent(
        icon = Icons.Default.Star,
        iconColor = NeonPink,
        title = "FREE & PREMIUM",
        subtitle = "Choose Your Protection Level",
        description = "Start with powerful free features, upgrade anytime for real-time cloud analysis and zero-day threat protection.",
        features = listOf(
            "✅ Free on-device ML scanning",
            "✅ Manual scans (3 daily)",
            "⭐ Premium: Unlimited scans",
            "⭐ Premium: Real-time protection"
        )
    )
}

@Composable
private fun OnboardingPagePermissions() {
    OnboardingPageContent(
        icon = Icons.Default.CheckCircle,
        iconColor = NeonCyan,
        title = "TRUST-FIRST SECURITY",
        subtitle = "Empower Your Protection",
        description = "To provide maximum security, Sentinel needs specific permissions to monitor for advanced threats like overlay attacks and spyware.",
        features = listOf(
            "🛡️ Accessibility: Detects invisible phishing overlays",
            "🌐 Network: Fingerprints suspicious traffic patterns",
            "📱 Packages: Scans all installed apps for hidden risks",
            "⚡ Real-time: Continuous background protection"
        )
    )
}

@Composable
private fun OnboardingPageContent(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    description: String,
    features: List<String>
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlowingCard(
            modifier = Modifier.fillMaxWidth(),
            glowColor = iconColor,
            cornerRadius = 24.dp,
            animated = true
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = iconColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Divider(color = iconColor.copy(alpha = 0.3f))

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    features.forEach { feature ->
                        FeatureItem(text = feature, color = iconColor)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}
