package dev.abbasian.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abbasian.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatEducationBottomSheet(
    threatType: String,
    onDismiss: () -> Unit
) {
    val educationContent = getEducationContent(threatType)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        contentColor = TextPrimary,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = educationContent.icon,
                        contentDescription = null,
                        tint = educationContent.color,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = educationContent.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = educationContent.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = TextSecondary.copy(alpha = 0.2f)
                )
            }
            
            // Description
            item {
                SectionCard(title = "What is this?") {
                    Text(
                        text = educationContent.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary.copy(alpha = 0.9f),
                        lineHeight = 22.sp
                    )
                }
            }
            
            // How it works
            item {
                SectionCard(title = "How does it work?") {
                    Text(
                        text = educationContent.howItWorks,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary.copy(alpha = 0.9f),
                        lineHeight = 22.sp
                    )
                }
            }
            
            // Warning signs
            item {
                SectionCard(title = "⚠️ Warning Signs") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        educationContent.warningSigns.forEach { sign ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", color = WarningOrange, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sign,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Protection tips
            item {
                SectionCard(title = "🛡️ How to Protect Yourself") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        educationContent.protectionTips.forEach { tip ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SafeGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
            
            // Action button
            item {
                CyberButton(
                    text = "Got It",
                    onClick = onDismiss,
                    variant = ButtonVariant.PRIMARY,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = DeepBlack.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = NeonCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

data class ThreatEducation(
    val title: String,
    val subtitle: String,
    val description: String,
    val howItWorks: String,
    val warningSigns: List<String>,
    val protectionTips: List<String>,
    val icon: ImageVector,
    val color: Color
)

private fun getEducationContent(threatType: String): ThreatEducation {
    return when {
        threatType.contains("Spyware", ignoreCase = true) -> ThreatEducation(
            title = "Spyware Detected",
            subtitle = "Privacy invasion malware",
            description = "Spyware is malicious software designed to secretly monitor your activities, steal personal information, and transmit it to attackers without your knowledge or consent.",
            howItWorks = "Spyware operates silently in the background, capturing keystrokes, screenshots, camera/microphone access, location data, messages, and browsing history. It often disguises itself as legitimate apps or hides within other software.",
            warningSigns = listOf(
                "Unusual battery drain or device heating",
                "Unexpected data usage spikes",
                "Camera/microphone indicator appearing when apps are closed",
                "Strange background noise during calls",
                "Apps requesting excessive permissions"
            ),
            protectionTips = listOf(
                "Uninstall suspicious apps immediately",
                "Review app permissions regularly and revoke unnecessary access",
                "Only download apps from official stores",
                "Keep your device and apps updated",
                "Use a reliable security app like Sentinel"
            ),
            icon = Icons.Default.Security,
            color = NeonPink
        )
        
        threatType.contains("Banking Trojan", ignoreCase = true) || 
        threatType.contains("Trojan", ignoreCase = true) -> ThreatEducation(
            title = "Banking Trojan",
            subtitle = "Financial credential theft malware",
            description = "Banking trojans are sophisticated malware that steal your banking credentials, credit card information, and financial data. They use overlay attacks to display fake login screens over legitimate banking apps.",
            howItWorks = "The trojan monitors when you open banking apps, then displays a pixel-perfect fake login screen. When you enter credentials, they're sent to attackers. It often uses accessibility services to bypass security measures and intercept SMS-based 2FA codes.",
            warningSigns = listOf(
                "Requests for accessibility service permissions",
                "Overlay screens appearing over banking apps",
                "Unexpected app installation requests",
                "Banking app behaving strangely",
                "Unauthorized transactions in your account"
            ),
            protectionTips = listOf(
                "Remove the app immediately and change all banking passwords",
                "Contact your bank to secure your accounts",
                "Never grant accessibility service permissions to unknown apps",
                "Enable biometric authentication for banking apps",
                "Monitor your account statements regularly"
            ),
            icon = Icons.Default.Dangerous,
            color = CriticalMagenta
        )
        
        threatType.contains("Ransomware", ignoreCase = true) -> ThreatEducation(
            title = "Ransomware",
            subtitle = "File encryption extortion malware",
            description = "Ransomware encrypts your files or locks your device, demanding payment (usually cryptocurrency) to restore access. Modern mobile ransomware can also steal data before encryption for double extortion.",
            howItWorks = "After installation, ransomware requests device administrator permissions. Once granted, it locks your screen with an unremovable overlay, encrypts files, or threatens to leak stolen personal data unless you pay a ransom.",
            warningSigns = listOf(
                "Request for device administrator permissions",
                "Sudden inability to access files or device",
                "Ransom message appearing on screen",
                "Files with strange extensions (.locked, .encrypted)",
                "Unable to boot into device normally"
            ),
            protectionTips = listOf(
                "DO NOT pay the ransom - it doesn't guarantee recovery",
                "Boot into Safe Mode and remove administrator permissions",
                "Factory reset device if necessary (ensure data is backed up)",
                "Report to law enforcement",
                "Regularly backup important data to cloud storage"
            ),
            icon = Icons.Default.Lock,
            color = CriticalMagenta
        )
        
        threatType.contains("Suspicious Lightweight", ignoreCase = true) ||
        threatType.contains("Size", ignoreCase = true) -> ThreatEducation(
            title = "Suspicious Lightweight App",
            subtitle = "Anomalous permission requests",
            description = "This app is unusually small (under 500KB) but requests sensitive permissions like camera, microphone, or storage access. Legitimate apps requiring these permissions are typically larger as they include proper UI and functionality.",
            howItWorks = "Malware authors create tiny apps to quickly deploy spying capabilities. The small size allows rapid distribution and reduces detection. These apps often lack legitimate functionality and exist solely to harvest data or spy on users.",
            warningSigns = listOf(
                "App size under 1MB requesting dangerous permissions",
                "Minimal or no visible functionality",
                "Generic app icon or name",
                "Requests permissions unrelated to stated purpose",
                "Poor grammar in app description"
            ),
            protectionTips = listOf(
                "Uninstall the app immediately",
                "Check app size before granting permissions",
                "Research apps before installation",
                "Read user reviews for legitimacy indicators",
                "Prefer well-established apps from known developers"
            ),
            icon = Icons.Default.Warning,
            color = WarningOrange
        )
        
        threatType.contains("Permission", ignoreCase = true) -> ThreatEducation(
            title = "Suspicious Permissions",
            subtitle = "Excessive or unusual access requests",
            description = "This app requests permission combinations that are unusual for its stated purpose. For example, a calculator app requesting camera and SMS access is highly suspicious.",
            howItWorks = "Malicious apps request broad permissions to maximize data collection capabilities. They exploit user trust and permission fatigue (users clicking 'Allow' without reading) to gain access they shouldn't have.",
            warningSigns = listOf(
                "Permissions unrelated to app functionality",
                "Multiple high-risk permissions requested",
                "Requests permissions immediately on launch",
                "App continues working if permissions denied (meaning it doesn't actually need them)",
                "Background permissions for apps that don't need them"
            ),
            protectionTips = listOf(
                "Review and revoke suspicious permissions in Settings",
                "Only grant permissions when actively using the feature",
                "Deny background location unless absolutely necessary",
                "Question why apps need certain permissions",
                "Use permission monitoring tools like Sentinel"
            ),
            icon = Icons.Default.Security,
            color = WarningOrange
        )
        
        else -> ThreatEducation(
            title = "Security Threat Detected",
            subtitle = "Potentially harmful application",
            description = "This app has been flagged by our security analysis for suspicious behavior or characteristics. It may pose a risk to your device security, privacy, or personal data.",
            howItWorks = "Security threats use various techniques to compromise devices: exploiting vulnerabilities, social engineering, permission abuse, or malicious code execution. They may steal data, install additional malware, or provide remote access to attackers.",
            warningSigns = listOf(
                "Unexpected behavior or crashes",
                "Unusual network activity",
                "Battery drain or performance issues",
                "Unwanted ads or redirects",
                "Difficulty uninstalling the app"
            ),
            protectionTips = listOf(
                "Uninstall suspicious apps immediately",
                "Run a full system scan",
                "Change passwords for sensitive accounts",
                "Monitor for unusual account activity",
                "Keep your security software updated"
            ),
            icon = Icons.Default.Security,
            color = NeonPink
        )
    }
}
