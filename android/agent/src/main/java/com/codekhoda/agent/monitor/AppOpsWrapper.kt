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
    val isForeground: Boolean,
    val isRunning: Boolean = false
)

@Singleton
class AppOpsWrapperImpl @Inject constructor(
    private val context: Context
) : AppOpsWrapper {

    override fun getRecentOps(durationMs: Long, targetOps: Array<String>): List<SimpleOpData> {
        val appOpsManager = context.getSystemService(AppOpsManager::class.java) ?: return emptyList()
        val result = mutableListOf<SimpleOpData>()
        val now = System.currentTimeMillis()

        try {
            // Reflectively call getPackagesForOps to avoid compile-time dependency on API 29+ symbols
            // which appear to be missing in the current build environment's android.jar
            val method = appOpsManager.javaClass.getMethod("getPackagesForOps", Array<String>::class.java)
            val packages = method.invoke(appOpsManager, targetOps) as? List<*> ?: return emptyList()

            for (pkgOps in packages) {
                if (pkgOps == null) continue
                
                // pkgOps is AppOpsManager.PackageOps
                val pkgName = pkgOps.javaClass.getMethod("getPackageName").invoke(pkgOps) as String
                val ops = pkgOps.javaClass.getMethod("getOps").invoke(pkgOps) as? List<*> ?: continue

                for (opEntry in ops) {
                    if (opEntry == null) continue
                    
                    // opEntry is AppOpsManager.OpEntry
                    val lastAccessTime = getLastAccessTimeReflect(opEntry)
                    
                    if (now - lastAccessTime < durationMs) {
                        val opStr = getOpStrReflect(opEntry)
                        
                        if (opStr.isNotEmpty()) {
                             result.add(SimpleOpData(
                                packageName = pkgName,
                                op = opStr,
                                timestamp = lastAccessTime,
                                duration = getDurationReflect(opEntry),
                                isForeground = isForegroundAccessReflect(opEntry),
                                isRunning = getIsRunningReflect(opEntry)
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        return result
    }

    private fun getLastAccessTimeReflect(opEntry: Any): Long {
        return try {
            // Try API 29+ method first: getLastAccessTime(int flags)
            val flags = 0x01 or 0x08 // OP_FLAG_SELF | OP_FLAG_TRUSTED_PROXIED
            opEntry.javaClass.getMethod("getLastAccessTime", Int::class.javaPrimitiveType).invoke(opEntry, flags) as Long
        } catch (e: Exception) {
            try {
                // Fallback to deprecated getTime()
                opEntry.javaClass.getMethod("getTime").invoke(opEntry) as Long
            } catch (e2: Exception) {
                0L
            }
        }
    }
    
    private fun getOpStrReflect(opEntry: Any): String {
        return try {
             opEntry.javaClass.getMethod("getOpStr").invoke(opEntry) as String
        } catch (e: Exception) {
            ""
        }
    }

    private fun getDurationReflect(opEntry: Any): Long {
         return try {
            val flags = 0x01 or 0x08
            opEntry.javaClass.getMethod("getLastDuration", Int::class.javaPrimitiveType).invoke(opEntry, flags) as Long
        } catch (e: Exception) {
             try {
                (opEntry.javaClass.getMethod("getDuration").invoke(opEntry) as Int).toLong()
            } catch (e2: Exception) {
                0L
            }
        }
    }

    private fun isForegroundAccessReflect(opEntry: Any): Boolean {
         return try {
             val timeForeground = opEntry.javaClass.getMethod("getLastAccessTime", Int::class.javaPrimitiveType)
                 .invoke(opEntry, 0x01 or 0x04) as Long
             val timeBackground = opEntry.javaClass.getMethod("getLastAccessTime", Int::class.javaPrimitiveType)
                 .invoke(opEntry, 0x01 or 0x10) as Long
             timeForeground >= timeBackground
        } catch (e: Exception) {
            true
        }
    }

    private fun getIsRunningReflect(opEntry: Any): Boolean {
        return try {
            opEntry.javaClass.getMethod("isRunning").invoke(opEntry) as Boolean
        } catch (e: Exception) {
            false
        }
    }
}
