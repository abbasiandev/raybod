package com.codekhoda.agent.monitor

import android.content.Context
import com.codekhoda.domain.model.AccessType
import com.codekhoda.domain.model.PermissionUsageEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundActivityTracker @Inject constructor(
    private val permissionUsageMonitor: PermissionUsageMonitor
) {

    companion object {
        private const val DEFAULT_MONITORING_DURATION_MS = 3600000L  // 1 hour
    }

    /**
     * Identifies apps accessing sensitive resources while in the background.
     * 
     * Background access to camera/microphone/location is a strong indicator
     * of spyware behavior, especially on Android 9+ where this is restricted.
     * 
     * @param durationMs Time window to analyze (default: 1 hour)
     * @return List of suspicious permission usage events
     */
    fun getSuspiciousBackgroundActivities(durationMs: Long = DEFAULT_MONITORING_DURATION_MS): List<PermissionUsageEvent> {
        val allPermissionEvents = permissionUsageMonitor.getRecentPermissionUsage(durationMs)
        
        // Filter for background usage of sensitive resources only
        return allPermissionEvents.filter { event ->
            !event.wasInForeground && isSensitiveResource(event.accessType)
        }
    }

    /**
     * Determines if an access type is sensitive enough to flag in background.
     * 
     * Camera and microphone in background are highly suspicious (potential spyware).
     * Location in background may be legitimate for navigation/fitness apps.
     */
    private fun isSensitiveResource(accessType: AccessType): Boolean {
        return when (accessType) {
            AccessType.CAMERA, 
            AccessType.MICROPHONE -> true  // Highly suspicious in background
            AccessType.LOCATION -> true     // Suspicious unless navigation/fitness app
            else -> false
        }
    }
}
