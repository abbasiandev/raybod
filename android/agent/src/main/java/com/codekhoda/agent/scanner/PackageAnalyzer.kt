package com.codekhoda.agent.scanner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.codekhoda.domain.filter.SystemPackageFilter
import com.codekhoda.domain.model.AppPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class PackageAnalyzer(private val context: Context) {

    suspend fun getInstalledApps(): List<AppPackage> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val packageInfoFlags = PackageManager.GET_PERMISSIONS or 
                               PackageManager.GET_SIGNATURES or 
                               PackageManager.GET_ACTIVITIES or 
                               PackageManager.GET_SERVICES or 
                               PackageManager.GET_RECEIVERS
                    
        val installedPackages = packageManager.getInstalledPackages(packageInfoFlags)
        
        installedPackages
            .filter { packageInfo -> !SystemPackageFilter.shouldExclude(packageInfo.packageName) }
            .map { packageInfo -> convertPackageInfo(packageInfo) }
    }

    suspend fun analyzePackage(packageName: String): AppPackage? = withContext(Dispatchers.IO) {
        try {
            val packageManager = context.packageManager
            val packageInfoFlags = PackageManager.GET_PERMISSIONS or 
                                   PackageManager.GET_SIGNATURES or 
                                   PackageManager.GET_ACTIVITIES or 
                                   PackageManager.GET_SERVICES or 
                                   PackageManager.GET_RECEIVERS
                        
            val packageInfo = packageManager.getPackageInfo(packageName, packageInfoFlags)
            convertPackageInfo(packageInfo)
        } catch (e: Exception) {
            null
        }
    }

    private fun convertPackageInfo(packageInfo: PackageInfo): AppPackage {
        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        val signature = getSignature(packageInfo)
        val intents = try {
            getListOfIntents(packageInfo.packageName)
        } catch (e: Exception) {
            emptyList<String>()
        }
        
        val nativeLibraries = getNativeLibraries(packageInfo)
        val (hasReflection, hasDynamicLoading) = analyzeSuspiciousApis(packageInfo)
        
        return AppPackage(
            packageName = packageInfo.packageName,
            versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode else packageInfo.versionCode.toLong(),
            versionName = packageInfo.versionName ?: "",
            signature = signature,
            permissions = permissions,
            intents = intents,
            installTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
            nativeLibraries = nativeLibraries,
            activityCount = packageInfo.activities?.size ?: 0,
            serviceCount = packageInfo.services?.size ?: 0,
            receiverCount = packageInfo.receivers?.size ?: 0,
            hasReflection = hasReflection,
            hasDynamicLoading = hasDynamicLoading,
            
            // Enhanced metadata for context-aware analysis
            appLabel = getAppLabel(packageInfo),
            category = getAppCategory(packageInfo),
            installedSize = getInstalledSize(packageInfo),
            targetSdkVersion = packageInfo.applicationInfo.targetSdkVersion,
            minSdkVersion = if (android.os.Build.VERSION.SDK_INT >= 24) packageInfo.applicationInfo.minSdkVersion else 0
        )
    }

    private fun getAppLabel(packageInfo: PackageInfo): String {
        return try {
            packageInfo.applicationInfo.loadLabel(context.packageManager).toString()
        } catch (e: Exception) {
            packageInfo.packageName
        }
    }

    private fun getAppCategory(packageInfo: PackageInfo): String {
        return if (android.os.Build.VERSION.SDK_INT >= 26) {
            when (packageInfo.applicationInfo.category) {
                android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Game"
                android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "Audio"
                android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "Video"
                android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "Photo"
                android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "News"
                android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "Navigation"
                android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                else -> ""
            }
        } else {
            ""
        }
    }

    private fun getInstalledSize(packageInfo: PackageInfo): Long {
        return try {
            java.io.File(packageInfo.applicationInfo.sourceDir).length()
        } catch (e: Exception) {
            0L
        }
    }

    private fun analyzeSuspiciousApis(packageInfo: PackageInfo): Pair<Boolean, Boolean> {
        // Detect reflection and dynamic code loading patterns
        // In production, this would involve DEX file analysis for patterns like:
        // - Ljava/lang/reflect/Method;->invoke
        // - DexClassLoader/PathClassLoader usage
        
        var reflectionFound = false
        var dynamicLoadingFound = false
        
        try {
            val sourceDir = packageInfo.applicationInfo.sourceDir
            val apkFile = java.io.File(sourceDir)
            if (apkFile.exists()) {
                // Heuristic: Scan first 50KB of APK for suspicious patterns
                // Many malware samples leave detectable strings even without full DEX analysis
                val bytes = apkFile.inputStream().use { it.readNBytes(1024 * 50) }
                val content = String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1)
                
                if (content.contains("DexClassLoader") || content.contains("PathClassLoader")) {
                    dynamicLoadingFound = true
                }
                if (content.contains("java/lang/reflect/Method") || content.contains("getDeclaredMethod")) {
                    reflectionFound = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return Pair(reflectionFound, dynamicLoadingFound)
    }

    private fun getNativeLibraries(packageInfo: PackageInfo): List<String> {
        val nativeLibraryList = mutableListOf<String>()
        try {
            val nativeLibDir = packageInfo.applicationInfo.nativeLibraryDir
            val libDirectory = java.io.File(nativeLibDir)
            if (libDirectory.exists() && libDirectory.isDirectory) {
                libDirectory.listFiles()?.forEach { libFile ->
                    if (libFile.name.endsWith(".so")) {
                        nativeLibraryList.add(libFile.name)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return nativeLibraryList
    }

    private fun getSignature(packageInfo: PackageInfo): String {
        return try {
             val signatures = packageInfo.signatures
             if (signatures != null && signatures.isNotEmpty()) {
                 hashString("SHA-256", signatures[0].toByteArray())
             } else {
                 ""
             }
        } catch (e: Exception) {
            ""
        }
    }

    private fun hashString(type: String, input: ByteArray): String {
        val bytes = MessageDigest.getInstance(type).digest(input)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getListOfIntents(packageName: String): List<String> {
        val intents = mutableListOf<String>()
        try {
            val packageContext = context.createPackageContext(packageName, 0)
            val am = packageContext.assets
            val addAssetPath = am.javaClass.getMethod("addAssetPath", String::class.java)
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val cookie = addAssetPath.invoke(am, appInfo.sourceDir) as Int
            val xmlParser = am.openXmlResourceParser(cookie, "AndroidManifest.xml")
            
            var eventType = xmlParser.next()
            while (eventType != android.content.res.XmlResourceParser.END_DOCUMENT) {
                if (eventType == android.content.res.XmlResourceParser.START_TAG && "action" == xmlParser.name) {
                    for (i in 0 until xmlParser.attributeCount) {
                        if (xmlParser.getAttributeName(i) == "name") {
                            intents.add(xmlParser.getAttributeValue(i))
                        }
                    }
                }
                eventType = xmlParser.next()
            }
            xmlParser.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return intents
    }
}
