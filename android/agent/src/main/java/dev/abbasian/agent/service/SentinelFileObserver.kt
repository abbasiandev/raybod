package dev.abbasian.agent.service

import android.os.FileObserver
import android.util.Log
import java.io.File

/**
 * Category 2.5: File System Watchdog
 * Monitors sensitive directories for unauthorized modifications,
 * such as malware trying to drop payloads in /data/local/tmp or 
 * encrypting files in public storage (Ransomware behavior).
 */
class SentinelFileObserver(path: String) : FileObserver(path, ALL_EVENTS) {

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return

        when (event and ALL_EVENTS) {
            CREATE -> {
                Log.d("FileWatchdog", "File created in sensitive location: $path")
                // Here we would trigger a scan of the new file if it's an executable or APK
                if (path.endsWith(".apk") || path.endsWith(".so") || path.endsWith(".dex")) {
                    Log.w("FileWatchdog", "SUSPICIOUS: Executable file dropped: $path")
                }
            }
            MODIFY -> {
                // Potential ransomware behavior: massive rapid modifications
                Log.d("FileWatchdog", "File modified: $path")
            }
            DELETE -> {
                Log.d("FileWatchdog", "File deleted: $path")
            }
        }
    }
    
    companion object {
        fun startWatchingSensitiveAreas() {
            // In a non-root environment, we are limited to app-specific or shared storage
            // This is a simulation of watching common "dropper" areas
            val pathsToWatch = listOf(
                "/sdcard/Download",
                "/sdcard/Android/data"
            )
            
            pathsToWatch.forEach { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        SentinelFileObserver(path).startWatching()
                        Log.i("FileWatchdog", "Started watching: $path")
                    }
                } catch (e: Exception) {
                    Log.e("FileWatchdog", "Failed to watch $path: ${e.message}")
                }
            }
        }
    }
}




