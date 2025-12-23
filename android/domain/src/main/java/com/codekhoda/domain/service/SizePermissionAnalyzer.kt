package com.codekhoda.domain.service

import com.codekhoda.domain.model.AppPackage

object SizePermissionAnalyzer {
    
    private const val SUSPICIOUS_SMALL_SIZE = 500 * 1024L // 500KB
    private const val VERY_LARGE_SIZE = 100 * 1024 * 1024L // 100MB
    private const val SUSPICIOUS_PERMISSION_COUNT = 10
    
    fun analyzeAnomalies(appPackage: AppPackage): List<String> {
        val anomalies = mutableListOf<String>()
        
        if (appPackage.installedSize <= 0) {
            return anomalies
        }
        
        val sizeKb = appPackage.installedSize / 1024
        val sizeMb = appPackage.installedSize / (1024 * 1024)
        
        // Rule 1: Tiny apps with dangerous permissions
        if (appPackage.installedSize < SUSPICIOUS_SMALL_SIZE) {
            val dangerousPerms = getDangerousPermissions(appPackage.permissions)
            
            if (dangerousPerms.contains("CAMERA")) {
                anomalies.add("Tiny app (${sizeKb}KB) requesting camera access")
            }
            if (dangerousPerms.contains("RECORD_AUDIO")) {
                anomalies.add("Tiny app (${sizeKb}KB) requesting microphone access")
            }
            if (dangerousPerms.any { it.contains("SMS") }) {
                anomalies.add("Tiny app (${sizeKb}KB) requesting SMS access")
            }
            if (dangerousPerms.any { it.contains("STORAGE") }) {
                anomalies.add("Tiny app (${sizeKb}KB) requesting storage access")
            }
            if (dangerousPerms.contains("ACCESS_FINE_LOCATION") || dangerousPerms.contains("ACCESS_COARSE_LOCATION")) {
                anomalies.add("Tiny app (${sizeKb}KB) requesting location access")
            }
        }
        
        // Rule 2: Very large apps (potential bloatware or data harvesting)
        if (appPackage.installedSize > VERY_LARGE_SIZE) {
            if (appPackage.permissions.size > SUSPICIOUS_PERMISSION_COUNT) {
                anomalies.add("Very large app (${sizeMb}MB) with ${appPackage.permissions.size} permissions")
            }
        }
        
        // Rule 3: Permission-to-size ratio anomaly
        val permissionDensity = appPackage.permissions.size.toFloat() / (sizeKb + 1)
        if (permissionDensity > 0.1f && appPackage.installedSize < 1024 * 1024) { // More than 1 permission per 10KB for apps under 1MB
            anomalies.add("Unusual permission density: ${appPackage.permissions.size} permissions in ${sizeKb}KB")
        }
        
        // Rule 4: Intent combo with small size
        if (appPackage.installedSize < SUSPICIOUS_SMALL_SIZE) {
            val suspiciousIntents = getSuspiciousIntents(appPackage.intents)
            if (suspiciousIntents.size >= 2) {
                anomalies.add("Tiny app (${sizeKb}KB) with suspicious intent combinations: ${suspiciousIntents.joinToString()}")
            }
        }
        
        return anomalies
    }
    
    private fun getDangerousPermissions(permissions: List<String>): List<String> {
        return permissions.map { it.substringAfterLast(".") }
            .filter { perm ->
                perm in setOf(
                    "CAMERA", "RECORD_AUDIO", "READ_CONTACTS", "WRITE_CONTACTS",
                    "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "ACCESS_BACKGROUND_LOCATION",
                    "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "MANAGE_EXTERNAL_STORAGE",
                    "READ_SMS", "SEND_SMS", "RECEIVE_SMS", "READ_CALL_LOG",
                    "READ_PHONE_STATE", "CALL_PHONE", "BODY_SENSORS"
                )
            }
    }
    
    private fun getSuspiciousIntents(intents: List<String>): List<String> {
        val suspiciousPatterns = setOf(
            "SENDTO", "SMS_RECEIVED", "BOOT_COMPLETED", "IMAGE_CAPTURE"
        )
        return intents.filter { intent ->
            suspiciousPatterns.any { pattern -> intent.contains(pattern) }
        }
    }
}
