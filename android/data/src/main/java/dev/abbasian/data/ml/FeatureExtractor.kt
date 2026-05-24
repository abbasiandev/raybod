package dev.abbasian.data.ml

import android.content.Context
import dev.abbasian.domain.model.AppPackage
import dev.abbasian.domain.model.DrebinFeatures
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
        var permissionFeatureList = mutableListOf<String>()
        var intentFeatureList = mutableListOf<String>()
        
        try {
            val gson = Gson()
            val listOfStringsType = TypeToken.getParameterized(List::class.java, String::class.java).type
            val mapType = TypeToken.getParameterized(Map::class.java, String::class.java, listOfStringsType).type
            val data: Map<String, List<String>> = gson.fromJson(json, mapType)
            
            permissionFeatureList = data["permissions"]?.toMutableList() ?: mutableListOf()
            intentFeatureList = data["intents"]?.toMutableList() ?: mutableListOf()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        permissionFeatures = permissionFeatureList
        intentFeatures = intentFeatureList
        
        // Ensure vector size matches model expectation
        vectorSize = 2000
    }

    fun extractFeatures(appPackage: AppPackage): FeatureAnalysisResult {
        val vector = FloatArray(vectorSize) { 0f }
        
        // Categorized DREBIN feature sets (8 categories for malware detection)
        val hardwareFeatures = mutableListOf<String>()              // S1: Hardware features
        val requestedPermissions = appPackage.permissions.toMutableList()  // S2: Requested permissions
        val appComponents = mutableListOf<String>()                 // S3: App components
        val filteredIntents = appPackage.intents.toMutableList()    // S4: Filtered intents
        val restrictedApis = mutableListOf<String>()                // S5: Restricted API calls
        val usedPermissions = mutableListOf<String>()               // S6: Actually used permissions (requires DEX analysis)
        val suspiciousApis = mutableListOf<String>()                // S7: Suspicious API patterns
        val networkAddresses = mutableListOf<String>()              // S8: Network addresses

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

        // 3. Populate app components (S3)
        if (appPackage.activityCount > 0) appComponents.add("Activities: ${appPackage.activityCount}")
        if (appPackage.serviceCount > 0) appComponents.add("Services: ${appPackage.serviceCount}")
        if (appPackage.receiverCount > 0) appComponents.add("Receivers: ${appPackage.receiverCount}")

        // 4. Populate hardware features (S1)
        if (appPackage.permissions.contains("android.permission.CAMERA")) hardwareFeatures.add("android.hardware.camera")
        if (appPackage.permissions.contains("android.permission.ACCESS_FINE_LOCATION")) hardwareFeatures.add("android.hardware.location.gps")
        if (appPackage.permissions.contains("android.permission.RECORD_AUDIO")) hardwareFeatures.add("android.hardware.microphone")

        // 5. Categorize heuristic patterns into suspicious APIs, restricted APIs, and network features
        // Detect suspicious package naming patterns
        if (appPackage.packageName.length < 5 || appPackage.packageName.split(".").size < 2) {
            suspiciousApis.add("Highly unusual package naming")
        }
        
        val maliciousPatterns = listOf(
            "metasploit", "spy", "stealer", "rat", "trojan", "hack", "payload",
            "botnet", "keylogger", "exploit", "ransom", "cryptolocker", "adload"
        )
        maliciousPatterns.forEach { pattern ->
            if (appPackage.packageName.contains(pattern, ignoreCase = true)) {
                suspiciousApis.add("Malicious string '$pattern' found in package name")
            }
        }

        val packageParts = appPackage.packageName.split(".")
        if (packageParts.any { it.length == 1 }) {
            suspiciousApis.add("Potential obfuscation (single-letter package part)")
        }

        val fakeSystemPatterns = listOf(
            "com.android.system.update", 
            "com.google.android.gm.system", 
            "android.security.update"
        )
        if (fakeSystemPatterns.any { appPackage.packageName.equals(it, ignoreCase = true) }) {
            suspiciousApis.add("Likely impersonation of system components")
        }

        // Check for suspicious native libraries
        if (appPackage.nativeLibraries.isNotEmpty()) {
            val suspiciousLibraryPatterns = listOf(
                "crypto", "ssl", "ssh", "payload", "shell", "proxy", "inject",
                "metasploit", "frida", "xposed", "substrate", "magisk"
            )
            appPackage.nativeLibraries.forEach { libraryName ->
                if (suspiciousLibraryPatterns.any { libraryName.contains(it, ignoreCase = true) }) {
                    suspiciousApis.add("Suspicious native library '$libraryName'")
                }
            }
        }

        // Detect restricted API usage
        if (appPackage.permissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
            restrictedApis.add("Accessibility Service (Potential UI Hijacking)")
        }
        
        // Detect structural anomalies (unusually high service ratio)
        val totalComponents = appPackage.activityCount + appPackage.serviceCount + appPackage.receiverCount
        if (totalComponents > 0) {
            val serviceRatio = appPackage.serviceCount.toFloat() / totalComponents.toFloat()
            if (serviceRatio > 0.8f && totalComponents > 5) {
                suspiciousApis.add("Extremely high service-to-component ratio")
            }
        }
        
        // Advanced evasion detection (reflection and dynamic code loading)
        if (appPackage.hasReflection) {
            suspiciousApis.add("Reflection usage detected (Potential Obfuscation)")
        }
        if (appPackage.hasDynamicLoading) {
            restrictedApis.add("Dynamic Code Loading (High Risk)")
        }

        // Network capability detection
        if (appPackage.permissions.contains("android.permission.INTERNET")) {
            networkAddresses.add("Internet communication enabled")
        }

        val drebinFeatures = DrebinFeatures(
            s1Hardware = hardwareFeatures,
            s2RequestedPermissions = requestedPermissions,
            s3AppComponents = appComponents,
            s4FilteredIntents = filteredIntents,
            s5RestrictedApis = restrictedApis,
            s6UsedPermissions = usedPermissions,
            s7SuspiciousApis = suspiciousApis,
            s8NetworkAddresses = networkAddresses
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
