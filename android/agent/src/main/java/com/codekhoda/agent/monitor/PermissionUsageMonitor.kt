package com.codekhoda.agent.monitor

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import com.codekhoda.domain.model.AccessType
import com.codekhoda.domain.model.PermissionUsageEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionUsageMonitor @Inject constructor(
    private val appOpsWrapper: AppOpsWrapper
) {
    fun getRecentPermissionUsage(durationMs: Long = 3600000): List<PermissionUsageEvent> { // Default 1 hour
        val targetOps = arrayOf(
            AppOpsManager.OPSTR_CAMERA,
            AppOpsManager.OPSTR_RECORD_AUDIO,
            AppOpsManager.OPSTR_FINE_LOCATION,
            AppOpsManager.OPSTR_COARSE_LOCATION
        )
        
        val ops = appOpsWrapper.getRecentOps(durationMs, targetOps)
        val events = mutableListOf<PermissionUsageEvent>()

        ops.forEach { opData ->
            val permission = opData.op
            val accessType = mapOpToAccessType(permission)
            
            events.add(PermissionUsageEvent(
                packageName = opData.packageName,
                permission = permission,
                timestamp = opData.timestamp,
                wasInForeground = opData.isForeground,
                durationMs = opData.duration,
                accessType = accessType
            ))
        }
        
        return events.sortedByDescending { it.timestamp }
    }

    private fun mapOpToAccessType(op: String): AccessType {
        return when (op) {
            AppOpsManager.OPSTR_CAMERA -> AccessType.CAMERA
            AppOpsManager.OPSTR_RECORD_AUDIO -> AccessType.MICROPHONE
            AppOpsManager.OPSTR_FINE_LOCATION, 
            AppOpsManager.OPSTR_COARSE_LOCATION -> AccessType.LOCATION
            else -> AccessType.UNKNOWN
        }
    }
}
