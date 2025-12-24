package com.codekhoda.domain.service

import com.codekhoda.domain.model.AppPackage

/**
 * Context-Aware Size-Permission Anomaly Analyzer
 * 
 * Version 2.0 - Significantly reduced false positives by:
 * - Considering app category when evaluating size
 * - Using realistic size thresholds (2MB instead of 500KB)
 * - Focusing on truly suspicious combinations
 * - Ignoring legitimate small app categories
 */
object SizePermissionAnalyzer {
    
    // UPDATED thresholds - more realistic for modern Android apps
    private const val SUSPICIOUS_SMALL_SIZE = 2 * 1024 * 1024L // 2MB (was 500KB)
    private const val VERY_LARGE_SIZE = 200 * 1024 * 1024L // 200MB (was 100MB)
    private const val SUSPICIOUS_PERMISSION_COUNT = 15 // Increased from 10
    
    // Legitimate small app categories (these can be under 2MB)
    private val LEGITIMATE_SMALL_CATEGORIES = setOf(
        "flashlight", "calculator", "clock", "alarm", "weather",
        "compass", "level", "ruler", "magnifier", "mirror",
        "widget", "launcher", "keyboard"
    )
    
    fun analyzeAnomalies(appPackage: AppPackage): List<String> {
        val anomalies = mutableListOf<String>()
        
        if (appPackage.installedSize <= 0) {
            return anomalies
        }
        
        val sizeKb = appPackage.installedSize / 1024
        val sizeMb = appPackage.installedSize / (1024 * 1024)
        
        // Check if this is a legitimate small app
        val isLegitimateSmallApp = isLegitimateSmallCategory(appPackage.packageName, appPackage.appLabel)
        
        // Rule 1: Very small apps with HIGH-RISK permissions (NOT just any dangerous permission)
        if (appPackage.installedSize < SUSPICIOUS_SMALL_SIZE && !isLegitimateSmallApp) {
            val dangerousPerms = getDangerousPermissions(appPackage.permissions)
            val highRiskPerms = getHighRiskPermissions(appPackage.permissions)
            
            // Only flag if requesting HIGH-RISK permissions (SMS, call monitoring, device admin)
            if (highRiskPerms.contains("SEND_SMS") || highRiskPerms.contains("RECEIVE_SMS")) {
                anomalies.add("Very small app (${sizeMb}MB) requesting SMS access")
            }
            if (highRiskPerms.contains("READ_CALL_LOG") || highRiskPerms.contains("PROCESS_OUTGOING_CALLS")) {
                anomalies.add("Very small app (${sizeMb}MB) requesting call monitoring")
            }
            if (highRiskPerms.contains("BIND_DEVICE_ADMIN")) {
                anomalies.add("Very small app (${sizeMb}MB) requesting device admin")
            }
            
            // Only flag storage/camera if COMBINED with other suspicious permissions
            val suspiciousComboCount = highRiskPerms.size
            if (suspiciousComboCount >= 2) {
                if (dangerousPerms.contains("CAMERA")) {
                    anomalies.add("Small app (${sizeMb}MB) with camera + suspicious permissions")
                }
                if (dangerousPerms.any { it.contains("STORAGE") }) {
                    anomalies.add("Small app (${sizeMb}MB) with storage + suspicious permissions")
                }
            }
        }
        
        // Rule 2: Very large apps (games and media apps are legitimately large)
        if (appPackage.installedSize > VERY_LARGE_SIZE) {
            val isLargeAppCategory = isLegitimatelyLargeCategory(appPackage.packageName, appPackage.appLabel)
            
            if (!isLargeAppCategory && appPackage.permissions.size > SUSPICIOUS_PERMISSION_COUNT) {
                anomalies.add("Unusually large app (${sizeMb}MB) with ${appPackage.permissions.size} permissions")
            }
        }
        
        // Rule 3: Permission-to-size ratio anomaly (UPDATED - more lenient)
        // Only flag if < 1MB with many permissions (not 1 permission per 10KB)
        if (appPackage.installedSize < 1024 * 1024) { // Under 1MB
            val dangerousPermCount = getDangerousPermissions(appPackage.permissions).size
            if (dangerousPermCount >= 5) { // 5+ dangerous permissions in <1MB app
                anomalies.add("Small app (${sizeMb}MB) with ${dangerousPermCount} dangerous permissions")
            }
        }
        
        // Rule 4: Intent combo with small size (UPDATED - more strict)
        if (appPackage.installedSize < SUSPICIOUS_SMALL_SIZE && !isLegitimateSmallApp) {
            val suspiciousIntents = getSuspiciousIntents(appPackage.intents)
            if (suspiciousIntents.size >= 3) { // Increased from 2 to 3
                anomalies.add("Small app (${sizeMb}MB) with suspicious intent combinations: ${suspiciousIntents.joinToString()}")
            }
        }
        
        return anomalies
    }
    
    private fun isLegitimateSmallCategory(packageName: String, appLabel: String): Boolean {
        val searchText = "$packageName $appLabel".lowercase()
        return LEGITIMATE_SMALL_CATEGORIES.any { category -> searchText.contains(category) }
    }
    
    private fun isLegitimatelyLargeCategory(packageName: String, appLabel: String): Boolean {
        val largeCategories = listOf("game", "video", "media", "movie", "netflix", "youtube", "music", "spotify")
        val searchText = "$packageName $appLabel".lowercase()
        return largeCategories.any { category -> searchText.contains(category) }
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
    
    /**
     * Returns HIGH-RISK permissions that are almost always suspicious in small apps.
     */
    private fun getHighRiskPermissions(permissions: List<String>): List<String> {
        return permissions.map { it.substringAfterLast(".") }
            .filter { perm ->
                perm in setOf(
                    "SEND_SMS", "RECEIVE_SMS", "READ_SMS",
                    "READ_CALL_LOG", "PROCESS_OUTGOING_CALLS",
                    "BIND_DEVICE_ADMIN", "BIND_ACCESSIBILITY_SERVICE",
                    "REQUEST_INSTALL_PACKAGES"
                )
            }
    }
    
    private fun getSuspiciousIntents(intents: List<String>): List<String> {
        // UPDATED: Removed IMAGE_CAPTURE (too common), kept truly suspicious ones
        val suspiciousPatterns = setOf(
            "SMS_RECEIVED", "BOOT_COMPLETED", "NEW_OUTGOING_CALL", "PHONE_STATE"
        )
        return intents.filter { intent ->
            suspiciousPatterns.any { pattern -> intent.contains(pattern) }
        }
    }
}
