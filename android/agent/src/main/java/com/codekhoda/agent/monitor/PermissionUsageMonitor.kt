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
    @ApplicationContext private val context: Context
) {
    private val appOpsManager = context.getSystemService(AppOpsManager::class.java)

    fun getRecentPermissionUsage(durationMs: Long = 3600000): List<PermissionUsageEvent> { // Default 1 hour
        // Temporarily disabled due to restricted AppOps API access issues in current build environment
        return emptyList()
    }

    /* Commented out problematic hidden API usage
    private fun getLastAccessTimeCompat(opEntry: AppOpsManager.OpEntry): Long {
        // ...
    }
    */
}
