package com.codekhoda.domain.service

import com.codekhoda.domain.model.AnomalyType
import com.codekhoda.domain.model.BehaviorAnomaly
import com.codekhoda.domain.model.PermissionUsageEvent

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BehaviorAnalysisEngine @Inject constructor() {

    /**
     * Detects mismatches between apparent user activity and permission usage.
     * 
     * @param currentForegroundApp The package name of the app currently visible to the user (if known).
     * @param recentPermissionEvents List of recent permission usage events to analyze.
     */
    fun detectActivityMismatch(
        currentForegroundApp: String,
        recentPermissionEvents: List<PermissionUsageEvent>
    ): List<BehaviorAnomaly> {
        val anomalies = mutableListOf<BehaviorAnomaly>()
        
        for (event in recentPermissionEvents) {
            // Rule 1: Background access to sensitive sensors (Mic/Camera)
            if (!event.wasInForeground && isSensistiveSensor(event.permission)) {
                anomalies.add(BehaviorAnomaly(
                    packageName = event.packageName,
                    anomalyType = AnomalyType.UNEXPECTED_BACKGROUND_ACCESS,
                    description = "App accessed ${getSimplePermissionName(event.permission)} while in background",
                    severity = 0.9f
                ))
            }
            // Rule 2: Foreground mismatch (User thinks they are in App A, but App B uses Camera)
            // Note: This is subtle. If App B is using camera in foreground (wasInForeground=true),
            // but the system says "currentForegroundApp" is App A, then App B might be an overlay or doing something tricky.
            // However, our PermissionUsageEvent 'wasInForeground' comes from AppOpsManager, which is usually accurate about process state.
            // If process was TOP, appOps says foreground.
            // So we mainly rely on Rule 1 for now.
        }
        
        return anomalies
    }

    private fun isSensistiveSensor(permission: String): Boolean {
        return permission.endsWith("CAMERA") || 
               permission.endsWith("RECORD_AUDIO") ||
               permission.endsWith("ACCESS_FINE_LOCATION")
    }
    
    private fun getSimplePermissionName(permission: String): String {
        return permission.substringAfterLast(".")
    }
}
