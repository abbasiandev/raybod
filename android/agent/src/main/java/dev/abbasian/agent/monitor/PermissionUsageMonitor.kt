package dev.abbasian.agent.monitor

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import dev.abbasian.domain.model.AccessType
import dev.abbasian.domain.model.PermissionUsageEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionUsageMonitor @Inject constructor(
    private val appOpsWrapper: AppOpsWrapper
) {

    companion object {
        private const val DEFAULT_DURATION_MS = 3600000L  // 1 hour
    }

    /**
     * Retrieves recent permission usage events for sensitive operations.
     * 
     * @param durationMs Time window to check (milliseconds)
     * @return List of permission usage events sorted by most recent first
     */
    fun getRecentPermissionUsage(durationMs: Long = DEFAULT_DURATION_MS): List<PermissionUsageEvent> {
        // Monitor critical permissions that are commonly abused
        val monitoredOperations = arrayOf(
            AppOpsManager.OPSTR_CAMERA,
            AppOpsManager.OPSTR_RECORD_AUDIO,
            AppOpsManager.OPSTR_FINE_LOCATION,
            AppOpsManager.OPSTR_COARSE_LOCATION
        )
        
        val recentOperations = appOpsWrapper.getRecentOps(durationMs, monitoredOperations)
        val permissionEvents = mutableListOf<PermissionUsageEvent>()

        recentOperations.forEach { operationData ->
            val permissionName = operationData.op
            val accessType = mapOpToAccessType(permissionName)
            
            permissionEvents.add(PermissionUsageEvent(
                packageName = operationData.packageName,
                permission = permissionName,
                timestamp = operationData.timestamp,
                wasInForeground = operationData.isForeground,
                durationMs = operationData.duration,
                accessType = accessType
            ))
        }
        
        return permissionEvents.sortedByDescending { it.timestamp }
    }

    /**
     * Maps AppOps operation strings to our domain AccessType enum.
     */
    private fun mapOpToAccessType(operationName: String): AccessType {
        return when (operationName) {
            AppOpsManager.OPSTR_CAMERA -> AccessType.CAMERA
            AppOpsManager.OPSTR_RECORD_AUDIO -> AccessType.MICROPHONE
            AppOpsManager.OPSTR_FINE_LOCATION, 
            AppOpsManager.OPSTR_COARSE_LOCATION -> AccessType.LOCATION
            else -> AccessType.UNKNOWN
        }
    }
}
