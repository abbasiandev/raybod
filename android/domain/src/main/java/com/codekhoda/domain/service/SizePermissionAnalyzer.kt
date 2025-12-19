package com.codekhoda.domain.service

import com.codekhoda.domain.model.AppPackage

/**
 * detection of "Suspicious Lightweight App" anomalies.
 * Example: A 500KB app requesting Dangerous permissions like Camera/Storage is suspicious.
 */
object SizePermissionAnalyzer {
    
    private const val SUSPICIOUS_SMALL_SIZE = 500 * 1024L // 500KB
    
    fun analyzeAnomalies(appPackage: AppPackage): List<String> {
        val anomalies = mutableListOf<String>()
        
        // Only analyze if size appears valid (greater than 0)
        if (appPackage.installedSize > 0 && appPackage.installedSize < SUSPICIOUS_SMALL_SIZE) {
            val sizeKb = appPackage.installedSize / 1024
            
            if (appPackage.permissions.contains("android.permission.READ_EXTERNAL_STORAGE") || 
                appPackage.permissions.contains("android.permission.WRITE_EXTERNAL_STORAGE")) {
                anomalies.add("Tiny app (${sizeKb}KB) requesting storage access")
            }
            if (appPackage.permissions.contains("android.permission.CAMERA")) {
                anomalies.add("Tiny app (${sizeKb}KB) requesting camera access")
            }
            if (appPackage.permissions.contains("android.permission.RECORD_AUDIO")) {
                anomalies.add("Tiny app (${sizeKb}KB) requesting microphone access")
            }
            if (appPackage.permissions.contains("android.permission.SEND_SMS") || 
                appPackage.permissions.contains("android.permission.RECEIVE_SMS")) {
                anomalies.add("Tiny app (${sizeKb}KB) requesting SMS access")
            }
        }
        
        return anomalies
    }
}
