package dev.abbasian.domain.service

import dev.abbasian.domain.model.AnomalyType
import dev.abbasian.domain.model.BehaviorAnomaly
import dev.abbasian.domain.model.PermissionUsageEvent

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
            
            // Rule 2: Foreground mismatch detection
            if (currentForegroundApp.isNotEmpty() && 
                event.wasInForeground && 
                event.packageName != currentForegroundApp &&
                isSensistiveSensor(event.permission)) {
                anomalies.add(BehaviorAnomaly(
                    packageName = event.packageName,
                    anomalyType = AnomalyType.UNEXPECTED_BACKGROUND_ACCESS,
                    description = "App ${event.packageName} accessed ${getSimplePermissionName(event.permission)} while user was in ${currentForegroundApp} (possible overlay attack)",
                    severity = 0.95f
                ))
            }
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
