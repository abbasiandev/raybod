package com.codekhoda.agent.monitor

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import com.codekhoda.agent.service.PermissionOverlayService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivePermissionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appOpsWrapper: AppOpsWrapper
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

        // Determine "active" as: accessed very recently with isRunning=true (API 29+) 
        // or just rely on Duration=0/isRunning check.
        // AppOpsWrapper.getRecentOps filters generally by time.
        // We'll ask for ops in last 1 second to capture "active" or "just finished"
        // But for "isRunning", checking existing active ops is distinct.
        // Our wrapper uses getPackagesForOps which returns status.
        
        val ops = appOpsWrapper.getRecentOps(1000, targetOps)
        
        val activeOp = ops.firstOrNull { it.isRunning }
        
        if (activeOp != null) {
            val intent = Intent(context, PermissionOverlayService::class.java).apply {
                action = PermissionOverlayService.ACTION_SHOW
                putExtra(PermissionOverlayService.EXTRA_PACKAGE, activeOp.packageName)
                putExtra(PermissionOverlayService.EXTRA_PERMISSION, activeOp.op)
                // TODO: Determine suspicion level based on Contextual Engine
                putExtra(PermissionOverlayService.EXTRA_SUSPICIOUS, false)
            }
            context.startService(intent)
        } else {
            val intent = Intent(context, PermissionOverlayService::class.java).apply {
                action = PermissionOverlayService.ACTION_HIDE
            }
            context.startService(intent)
        }
    }
}
