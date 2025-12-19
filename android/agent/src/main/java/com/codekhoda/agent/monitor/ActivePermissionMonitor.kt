package com.codekhoda.agent.monitor

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import com.codekhoda.agent.service.PermissionOverlayService
import com.codekhoda.domain.model.PermissionUsageEvent
import com.codekhoda.domain.service.BehaviorAnalysisEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivePermissionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appOpsWrapper: AppOpsWrapper,
    private val behaviorAnalysisEngine: BehaviorAnalysisEngine
) {

    /**
     * Checks for currently running sensitive permissions and triggers the overlay if found.
     * Should be called periodically (e.g., every 1-2 seconds) when monitoring is active.
     */
    fun checkAndNotify() {
        val targetOps = arrayOf(
            AppOpsManager.OPSTR_CAMERA,
            AppOpsManager.OPSTR_RECORD_AUDIO,
            AppOpsManager.OPSTR_FINE_LOCATION,
            AppOpsManager.OPSTR_COARSE_LOCATION
        )

        val ops = appOpsWrapper.getRecentOps(1000, targetOps)
        
        val activeOp = ops.firstOrNull { it.isRunning }
        
        if (activeOp != null) {
            // Map to Domain Event for Analysis
            // Note: simpleOpData.op is an OPSTR (e.g. "android:camera").
            val permission = getPermissionFromOp(activeOp.op) ?: activeOp.op
            
            val event = PermissionUsageEvent(
                packageName = activeOp.packageName,
                permission = permission,
                timestamp = activeOp.timestamp,
                wasInForeground = activeOp.isForeground,
                durationMs = activeOp.duration
            )
            
            // Detect Anomalies (pass single event as list)
            // currentForegroundApp is unknown ("") but Rule 1 doesn't need it.
            val anomalies = behaviorAnalysisEngine.detectActivityMismatch("", listOf(event))
            val isSuspicious = anomalies.isNotEmpty()

            val intent = Intent(context, PermissionOverlayService::class.java).apply {
                action = PermissionOverlayService.ACTION_SHOW
                putExtra(PermissionOverlayService.EXTRA_PACKAGE, activeOp.packageName)
                putExtra(PermissionOverlayService.EXTRA_PERMISSION, activeOp.op)
                putExtra(PermissionOverlayService.EXTRA_SUSPICIOUS, isSuspicious)
            }
            context.startService(intent)
        } else {
            val intent = Intent(context, PermissionOverlayService::class.java).apply {
                action = PermissionOverlayService.ACTION_HIDE
            }
            context.startService(intent)
        }
    }

    private fun getPermissionFromOp(op: String): String? {
        return when (op) {
            AppOpsManager.OPSTR_CAMERA -> "android.permission.CAMERA"
            AppOpsManager.OPSTR_RECORD_AUDIO -> "android.permission.RECORD_AUDIO"
            AppOpsManager.OPSTR_FINE_LOCATION -> "android.permission.ACCESS_FINE_LOCATION"
            AppOpsManager.OPSTR_COARSE_LOCATION -> "android.permission.ACCESS_COARSE_LOCATION"
            else -> null
        }
    }
}
