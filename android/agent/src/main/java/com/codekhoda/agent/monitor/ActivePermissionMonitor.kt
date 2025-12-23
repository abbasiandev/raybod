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

    companion object {
        private const val ACTIVE_CHECK_WINDOW_MS = 1000L  // Check last 1 second
    }

    /**
     * Checks for currently active sensitive permission usage and displays overlay notification.
     * 
     * Should be called periodically (every 1-2 seconds) when real-time monitoring is active.
     * Triggers visual overlay to alert user of active camera/microphone/location usage.
     */
    fun checkAndNotify() {
        val monitoredOperations = arrayOf(
            AppOpsManager.OPSTR_CAMERA,
            AppOpsManager.OPSTR_RECORD_AUDIO,
            AppOpsManager.OPSTR_FINE_LOCATION,
            AppOpsManager.OPSTR_COARSE_LOCATION
        )

        val ops = appOpsWrapper.getRecentOps(1000, monitoredOperations)
        val activeOp = ops.firstOrNull { it.isRunning }
        
        if (activeOp != null) {
            val permission = getPermissionFromOp(activeOp.op) ?: activeOp.op
            
            val event = PermissionUsageEvent(
                packageName = activeOp.packageName,
                permission = permission,
                timestamp = activeOp.timestamp,
                wasInForeground = activeOp.isForeground,
                durationMs = activeOp.duration
            )
            
            val currentForegroundApp = getCurrentForegroundApp()
            val anomalies = behaviorAnalysisEngine.detectActivityMismatch(currentForegroundApp, listOf(event))
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
    
    private fun getCurrentForegroundApp(): String {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                activityManager.getRunningTasks(1).firstOrNull()?.topActivity?.packageName ?: ""
            } else {
                @Suppress("DEPRECATION")
                activityManager.getRunningTasks(1).firstOrNull()?.topActivity?.packageName ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Maps AppOps operation strings to Android permission strings.
     */
    private fun getPermissionFromOp(operationString: String): String? {
        return when (operationString) {
            AppOpsManager.OPSTR_CAMERA -> "android.permission.CAMERA"
            AppOpsManager.OPSTR_RECORD_AUDIO -> "android.permission.RECORD_AUDIO"
            AppOpsManager.OPSTR_FINE_LOCATION -> "android.permission.ACCESS_FINE_LOCATION"
            AppOpsManager.OPSTR_COARSE_LOCATION -> "android.permission.ACCESS_COARSE_LOCATION"
            else -> null
        }
    }
}
