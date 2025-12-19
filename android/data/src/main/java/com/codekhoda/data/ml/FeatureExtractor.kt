package com.codekhoda.data.ml

import android.content.Context
import com.codekhoda.domain.model.AppPackage
import com.codekhoda.domain.model.DrebinFeatures
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val drebinFeatures: DrebinFeatures
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeatureAnalysisResult

        if (!featureVector.contentEquals(other.featureVector)) return false
        if (drebinFeatures != other.drebinFeatures) return false

        return true
    }

    override fun hashCode(): Int {
        var result = featureVector.contentHashCode()
        result = 31 * result + drebinFeatures.hashCode()
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
        
        // Categorized DREBIN sets
        val s1 = mutableListOf<String>()
        val s2 = appPackage.permissions.toMutableList() // S2 is requested permissions
        val s3 = mutableListOf<String>()
        val s4 = appPackage.intents.toMutableList()    // S4 is filtered intents
        val s5 = mutableListOf<String>()
        val s6 = mutableListOf<String>() // To be derived from S5 (requires deep dex analysis)
        val s7 = mutableListOf<String>()
        val s8 = mutableListOf<String>()

        // 1. Map Permissions (S2)
        permissionFeatures.forEachIndexed { index, feature ->
            if (appPackage.permissions.contains(feature)) {
                if (index < vectorSize) {
                    vector[index] = 1f
                }
            }
        }
        
        // 2. Map Intents (S4)
        val intentOffset = permissionFeatures.size
        intentFeatures.forEachIndexed { index, feature ->
            if (appPackage.intents.contains(feature)) {
                val targetIndex = intentOffset + index
                if (targetIndex < vectorSize) {
                    vector[targetIndex] = 1f
                }
            }
        }

        // 3. Populate S3 (Components)
        if (appPackage.activityCount > 0) s3.add("Activities: ${appPackage.activityCount}")
        if (appPackage.serviceCount > 0) s3.add("Services: ${appPackage.serviceCount}")
        if (appPackage.receiverCount > 0) s3.add("Receivers: ${appPackage.receiverCount}")

        // 4. Populate S1 (Hardware)
        if (appPackage.permissions.contains("android.permission.CAMERA")) s1.add("android.hardware.camera")
        if (appPackage.permissions.contains("android.permission.ACCESS_FINE_LOCATION")) s1.add("android.hardware.location.gps")
        if (appPackage.permissions.contains("android.permission.RECORD_AUDIO")) s1.add("android.hardware.microphone")

        // 5. Categorize existing heuristic logic into S5/S7/S8
        
        // Literal strings / Obfuscation -> S7 (Suspicious Patterns)
        if (appPackage.packageName.length < 5 || appPackage.packageName.split(".").size < 2) {
            s7.add("Highly unusual package naming")
        }
        
        val maliciousPatterns = listOf(
            "metasploit", "spy", "stealer", "rat", "trojan", "hack", "payload",
            "botnet", "keylogger", "exploit", "ransom", "cryptolocker", "adload"
        )
        maliciousPatterns.forEach { pattern ->
            if (appPackage.packageName.contains(pattern, ignoreCase = true)) {
                s7.add("Malicious string '$pattern' found in package name")
            }
        }

        val parts = appPackage.packageName.split(".")
        if (parts.any { it.length == 1 }) {
            s7.add("Potential obfuscation (single-letter package part)")
        }

        val fakeSystemPatterns = listOf("com.android.system.update", "com.google.android.gm.system", "android.security.update")
        if (fakeSystemPatterns.any { appPackage.packageName.equals(it, ignoreCase = true) }) {
            s7.add("Likely impersonation of system components")
        }

        // Native Libraries -> S7
        if (appPackage.nativeLibraries.isNotEmpty()) {
            val suspiciousLibs = listOf(
                "crypto", "ssl", "ssh", "payload", "shell", "proxy", "inject",
                "metasploit", "frida", "xposed", "substrate", "magisk"
            )
            appPackage.nativeLibraries.forEach { lib ->
                if (suspiciousLibs.any { lib.contains(it, ignoreCase = true) }) {
                    s7.add("Suspicious native library '$lib'")
                }
            }
        }

        // Restricted APIs (Simulated) -> S5
        if (appPackage.permissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
            s5.add("Accessibility Service (Potential UI Hijacking)")
        }
        
        // Structural Outliers -> S7
        val totalComponents = appPackage.activityCount + appPackage.serviceCount + appPackage.receiverCount
        if (totalComponents > 0) {
            val serviceRatio = appPackage.serviceCount.toFloat() / totalComponents.toFloat()
            if (serviceRatio > 0.8f && totalComponents > 5) {
                s7.add("Extremely high service-to-component ratio")
            }
        }
        
        // Category 2.1: Advanced Evasion Detection
        if (appPackage.hasReflection) {
            s7.add("Reflection usage detected (Potential Obfuscation)")
        }
        if (appPackage.hasDynamicLoading) {
            s5.add("Dynamic Code Loading (High Risk)")
        }

        // Network Addresses -> S8
        if (appPackage.permissions.contains("android.permission.INTERNET")) {
            s8.add("Internet communication enabled")
        }

        val drebinFeatures = DrebinFeatures(
            s1Hardware = s1,
            s2RequestedPermissions = s2,
            s3AppComponents = s3,
            s4FilteredIntents = s4,
            s5RestrictedApis = s5,
            s6UsedPermissions = s6,
            s7SuspiciousApis = s7,
            s8NetworkAddresses = s8
        )

        return FeatureAnalysisResult(vector, drebinFeatures)
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
