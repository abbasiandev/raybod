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

    /**
     * Identifies apps that are accessing sensitive resources while in the background.
     * This is a high-risk indicator (e.g., Spyware recording audio).
     */
    fun getSuspiciousBackgroundActivities(durationMs: Long = 3600000): List<PermissionUsageEvent> {
        val allEvents = permissionUsageMonitor.getRecentPermissionUsage(durationMs)
        
        return allEvents.filter { event ->
            !event.wasInForeground && isSensitiveResource(event.accessType)
        }
    }

    private fun isSensitiveResource(accessType: AccessType): Boolean {
        return when (accessType) {
            AccessType.CAMERA, 
            AccessType.MICROPHONE -> true // Highly suspicious in background (Android 9+ blocks this mostly, but bypasses exist)
            AccessType.LOCATION -> true // Suspicious unless it's a navigation/fitness app
            else -> false
        }
    }
}
