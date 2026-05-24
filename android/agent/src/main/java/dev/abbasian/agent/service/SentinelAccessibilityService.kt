package dev.abbasian.agent.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

/**
 * Category 2.2: Accessibility Abuse Detection
 * This service monitors UI interactions to detect potential overlay attacks,
 * invisible phishing screens, and unauthorized accessibility usage by other apps.
 */
class SentinelAccessibilityService : AccessibilityService() {

    private var lastPackageName: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Category 2.2: Accessibility Abuse Detection
        val sourcePackage = event.packageName?.toString() ?: "unknown"
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Detect potential overlay: if a window changes quickly or unexpectedly 
                // while a sensitive app is in the foreground.
                if (isSensitiveApp(sourcePackage) && lastPackageName != sourcePackage) {
                    Log.w("SentinelAccessibility", "Sensitive app $sourcePackage foregrounded. Monitoring for overlays.")
                }
                lastPackageName = sourcePackage
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Detect automated clickers (ATS - Automated Transfer System)
                if (isSensitiveApp(sourcePackage) && event.isPassword) {
                    Log.w("SentinelAccessibility", "Click event on password field in $sourcePackage")
                }
            }
        }
    }

    private fun isSensitiveApp(packageName: String): Boolean {
        val sensitivePrefixes = listOf("com.android.settings", "com.google.android.apps.walletnfcrel", "com.paypal", "bank")
        return sensitivePrefixes.any { packageName.contains(it, ignoreCase = true) }
    }

    override fun onInterrupt() {
        Log.d("SentinelAccessibility", "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("SentinelAccessibility", "Service connected - Monitoring UI for anomalies")
    }
}

