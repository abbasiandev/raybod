package com.codekhoda.agent.monitor

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around AppOpsManager to facilitate unit testing
 * and abstraction of API-level differences.
 */
interface AppOpsWrapper {
    fun getRecentOps(durationMs: Long, targetOps: Array<String>): List<SimpleOpData>
}

data class SimpleOpData(
    val packageName: String,
    val op: String,
    val timestamp: Long,
    val duration: Long,
    val isForeground: Boolean
)

@Singleton
class AppOpsWrapperImpl @Inject constructor(
    private val context: Context
) : AppOpsWrapper {

    private val appOpsManager = context.getSystemService(AppOpsManager::class.java)

    override fun getRecentOps(durationMs: Long, targetOps: Array<String>): List<SimpleOpData> {
        val manager = appOpsManager ?: return emptyList()
        val ops = manager.getPackagesForOps(targetOps) ?: return emptyList()
        val result = mutableListOf<SimpleOpData>()
        val now = System.currentTimeMillis()

        ops.forEach { pkgOps ->
            val packageName = pkgOps.packageName
            pkgOps.ops.forEach { opEntry ->
                val lastAccessTime = getLastAccessTimeCompat(opEntry)
                
                if (now - lastAccessTime < durationMs) {
                    // opEntry.opStr available in API 29. Fallback handled or assumed for compilation? 
                    // opStr is property.
                    val opStr = try { opEntry.opStr } catch (e: NoSuchFieldError) { "" }
                    
                    if (opStr.isNotEmpty()) {
                         result.add(SimpleOpData(
                            packageName = packageName,
                            op = opStr,
                            timestamp = lastAccessTime,
                            duration = getDurationCompat(opEntry),
                            isForeground = isForegroundAccess(opEntry)
                        ))
                    }
                }
            }
        }
        return result
    }

    private fun getLastAccessTimeCompat(opEntry: AppOpsManager.OpEntry): Long {
        return if (Build.VERSION.SDK_INT >= 29) {
            // Use reflection for flags to avoid compilation errors if symbols missing
            // 0x01 | 0x08 = OP_FLAG_SELF | OP_FLAG_TRUSTED_PROXIED
            opEntry.getLastAccessTime(0x01 or 0x08)
        } else {
            @Suppress("DEPRECATION")
            opEntry.time
        }
    }
    
    private fun getDurationCompat(opEntry: AppOpsManager.OpEntry): Long {
         return if (Build.VERSION.SDK_INT >= 29) {
             // 0x01 | 0x08
            opEntry.getLastDuration(0x01 or 0x08)
        } else {
            @Suppress("DEPRECATION")
            opEntry.duration.toLong()
        }
    }

    private fun isForegroundAccess(opEntry: AppOpsManager.OpEntry): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
             // OP_FLAG_TRUSTED_FOREGROUND = 0x04
             // OP_FLAG_TRUSTED_BACKGROUND = 0x10
             val timeForeground = opEntry.getLastAccessTime(0x01 or 0x04)
             val timeBackground = opEntry.getLastAccessTime(0x01 or 0x10)
             return timeForeground >= timeBackground
        }
        return true 
    }
}
