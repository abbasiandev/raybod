package com.codekhoda.data.ml

import android.content.Context
import com.codekhoda.domain.model.AppPackage
import dagger.hilt.android.qualifiers.ApplicationContext
import com.codekhoda.data.ml.FeatureAnalysisResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class to hold both the raw vector for TFLite and the human-readable features for UI.
 */
data class FeatureAnalysisResult(
    val featureVector: FloatArray,
    val matchedFeatures: List<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeatureAnalysisResult

        if (!featureVector.contentEquals(other.featureVector)) return false
        if (matchedFeatures != other.matchedFeatures) return false

        return true
    }

    override fun hashCode(): Int {
        var result = featureVector.contentHashCode()
        result = 31 * result + matchedFeatures.hashCode()
        return result
    }
}

@Singleton
class FeatureExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val permissionFeatures: List<String>
    private val intentFeatures: List<String>
    val vectorSize: Int

    init {
        val json = loadJSONFromAsset("features.json")
        var pList = mutableListOf<String>()
        var iList = mutableListOf<String>()
        
        try {
            val gson = Gson()
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            val data: Map<String, List<String>> = gson.fromJson(json, type)
            
            pList = data["permissions"]?.toMutableList() ?: mutableListOf()
            iList = data["intents"]?.toMutableList() ?: mutableListOf()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        permissionFeatures = pList
        intentFeatures = iList
        
        // Ensure vector size matches model expectation
        vectorSize = 2000
    }

    fun extractFeatures(appPackage: AppPackage): FeatureAnalysisResult {
        val vector = FloatArray(vectorSize) { 0f }
        val matchedFeatures = mutableListOf<String>()
        
        // 1. Map Permissions
        // Reference uses direct index mapping
        permissionFeatures.forEachIndexed { index, feature ->
            if (appPackage.permissions.contains(feature)) {
                if (index < vectorSize) {
                    vector[index] = 1f
                    matchedFeatures.add("Permission: $feature")
                }
            }
        }
        
        // 2. Map Intents
        // Reference code: inputVal[i + 489] = 1; where 489 is likely p_jArray.length()
        // We will use permissionFeatures.size as the offset to be dynamic but logically consistent
        val intentOffset = permissionFeatures.size // Should be 489 if features.json is same
        
        intentFeatures.forEachIndexed { index, feature ->
            if (appPackage.intents.contains(feature)) {
                val targetIndex = intentOffset + index
                if (targetIndex < vectorSize) {
                    vector[targetIndex] = 1f
                    matchedFeatures.add("Intent: $feature")
                }
            }
        }
        
        // 3. String Analysis: Obfuscation and Suspicious Components (Category 1.3)
        // Check if package name itself is suspicious (redundant but good for vector weight)
        if (appPackage.packageName.length < 5 || appPackage.packageName.split(".").size < 2) {
            matchedFeatures.add("Literal: Highly unusual package naming")
        }

        // Logic for known malicious strings in package/class names
        val maliciousPatterns = listOf(
            "metasploit", "spy", "stealer", "rat", "trojan", "hack", "payload",
            "botnet", "keylogger", "exploit", "ransom", "cryptolocker", "adload"
        )
        maliciousPatterns.forEach { pattern ->
            if (appPackage.packageName.contains(pattern, ignoreCase = true)) {
                matchedFeatures.add("Literal: Malicious string '$pattern' found in package name")
            }
        }

        // Check for common obfuscation patterns (e.g., single letter class/package names)
        val parts = appPackage.packageName.split(".")
        if (parts.any { it.length == 1 }) {
            matchedFeatures.add("Heuristic: Potential obfuscation (single-letter package part)")
        }

        // Detection of "Generic" or "Fake" system apps
        val fakeSystemPatterns = listOf("com.android.system.update", "com.google.android.gm.system", "android.security.update")
        if (fakeSystemPatterns.any { appPackage.packageName.equals(it, ignoreCase = true) }) {
            matchedFeatures.add("Risk: Likely impersonation of system components")
        }

        // 4. Native Library Fingerprinting (Category 1.2)
        if (appPackage.nativeLibraries.isNotEmpty()) {
            matchedFeatures.add("Native: App contains ${appPackage.nativeLibraries.size} native libraries")
            
            val suspiciousLibs = listOf(
                "crypto", "ssl", "ssh", "payload", "shell", "proxy", "inject",
                "metasploit", "frida", "xposed", "substrate", "magisk"
            )
            appPackage.nativeLibraries.forEach { lib ->
                if (suspiciousLibs.any { lib.contains(it, ignoreCase = true) }) {
                    matchedFeatures.add("Native: Suspicious library '$lib' detected")
                }
            }
        }

        // 5. Accessibility Abuse Awareness (Category 2.2)
        if (appPackage.permissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
            matchedFeatures.add("Risk: Accessibility Service detected (Potential UI Hijacking)")
            
            // Check for potential overlay or information theft patterns
            if (appPackage.permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")) {
                matchedFeatures.add("Risk: Critical combination: Accessibility + Overlay Alert (Phishing Risk)")
            }
            if (appPackage.permissions.contains("android.permission.QUERY_ALL_PACKAGES")) {
                matchedFeatures.add("Risk: Critical combination: Accessibility + Package Scanning (Spyware Risk)")
            }
        }

        // 6. Structural Analysis: Component Counts (Category 4.3)
        if (appPackage.activityCount == 0 && (appPackage.serviceCount > 0 || appPackage.receiverCount > 0)) {
            matchedFeatures.add("Structural: No activities but has services/receivers (Stealth/Background profile)")
        }
        
        // Category 3.2: Isolation Forest (Simulated Outlier Detection)
        // Detect apps that have an extremely unusual ratio of components compared to standard apps
        val totalComponents = appPackage.activityCount + appPackage.serviceCount + appPackage.receiverCount
        if (totalComponents > 0) {
            val serviceRatio = appPackage.serviceCount.toFloat() / totalComponents.toFloat()
            if (serviceRatio > 0.8f && totalComponents > 5) {
                matchedFeatures.add("Outlier: Extremely high service-to-component ratio (Background-heavy)")
            }
        }
        
        if (appPackage.receiverCount > 10) {
            matchedFeatures.add("Structural: Excessively high broadcast receiver count (${appPackage.receiverCount})")
        }
        
        // 7. Intent Filter Red Flags (Category 4.2)
        val highRiskIntents = listOf(
            "android.intent.action.BOOT_COMPLETED",
            "android.provider.Telephony.SMS_RECEIVED",
            "android.intent.action.PACKAGE_REPLACED",
            "android.intent.action.NEW_OUTGOING_CALL",
            "android.intent.action.DEVICE_ADMIN_ENABLED"
        )
        
        highRiskIntents.forEach { intent ->
            if (appPackage.intents.contains(intent)) {
                // Check if paired with internet for exfiltration
                if (appPackage.permissions.contains("android.permission.INTERNET")) {
                    val shortName = intent.substringAfterLast('.')
                    matchedFeatures.add("Risk: $shortName + Internet (Exfiltration Channel)")
                }
            }
        }

        // 8. Manifest Deep Dive (Category 1.5)
        if (appPackage.permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")) {
            matchedFeatures.add("Manifest: Can request installation of other packages (Dropper risk)")
        }
        if (appPackage.permissions.contains("android.permission.RECEIVE_BOOT_COMPLETED") && 
            appPackage.permissions.contains("android.permission.FOREGROUND_SERVICE")) {
            matchedFeatures.add("Manifest: Persistence pattern: Boot + Foreground Service")
        }
        
        // 9. Certificate Chain Analysis (Category 1.6)
        val debugSignatures = listOf(
            "3082030d308201f5a00302010202044e13", // Common debug signature prefix
            "androiddebugkey"
        )
        if (debugSignatures.any { appPackage.signature.contains(it, ignoreCase = true) }) {
            matchedFeatures.add("Cert: App signed with a known Debug key")
        }

        return FeatureAnalysisResult(vector, matchedFeatures)
    }

    private fun loadJSONFromAsset(filename: String): String {
        return try {
            val inputStream: InputStream = context.assets.open(filename)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (e: Exception) {
            e.printStackTrace()
            "{}"
        }
    }
}

