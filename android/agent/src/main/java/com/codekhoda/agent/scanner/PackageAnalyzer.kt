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
        val pm = context.packageManager
        val flags = PackageManager.GET_PERMISSIONS or 
                    PackageManager.GET_SIGNATURES or 
                    PackageManager.GET_ACTIVITIES or 
                    PackageManager.GET_SERVICES or 
                    PackageManager.GET_RECEIVERS
                    
        val packages = pm.getInstalledPackages(flags)
        
        packages
            .filter { pkg -> !SystemPackageFilter.shouldExclude(pkg.packageName) }
            .map { pkg -> convertPackageInfo(pkg) }
    }

    suspend fun analyzePackage(packageName: String): AppPackage? = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val flags = PackageManager.GET_PERMISSIONS or 
                        PackageManager.GET_SIGNATURES or 
                        PackageManager.GET_ACTIVITIES or 
                        PackageManager.GET_SERVICES or 
                        PackageManager.GET_RECEIVERS
                        
            val pkg = pm.getPackageInfo(packageName, flags)
            convertPackageInfo(pkg)
        } catch (e: Exception) {
            null
        }
    }

    private fun convertPackageInfo(pkg: PackageInfo): AppPackage {
        val permissions = pkg.requestedPermissions?.toList() ?: emptyList()
        val signature = getSignature(pkg)
        val intents = try {
            getListOfIntents(pkg.packageName)
        } catch (e: Exception) {
            emptyList<String>()
        }
        
        val nativeLibs = getNativeLibraries(pkg)
        val (hasReflection, hasDynamicLoading) = analyzeSuspiciousApis(pkg)
        
        return AppPackage(
            packageName = pkg.packageName,
            versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) pkg.longVersionCode else pkg.versionCode.toLong(),
            versionName = pkg.versionName ?: "",
            signature = signature,
            permissions = permissions,
            intents = intents,
            installTime = pkg.firstInstallTime,
            lastUpdateTime = pkg.lastUpdateTime,
            nativeLibraries = nativeLibs,
            activityCount = pkg.activities?.size ?: 0,
            serviceCount = pkg.services?.size ?: 0,
            receiverCount = pkg.receivers?.size ?: 0,
            hasReflection = hasReflection,
            hasDynamicLoading = hasDynamicLoading
        )
    }

    private fun analyzeSuspiciousApis(pkg: PackageInfo): Pair<Boolean, Boolean> {
        // In a real scenario, this would involve scanning the DEX files for 
        // Ljava/lang/reflect/Method;->invoke or Landroidx/loader/content/CursorLoader; etc.
        // For this implementation, we simulate detection by looking for common 
        // libraries or patterns in the application info.
        
        var reflectionFound = false
        var dynamicLoadingFound = false
        
        try {
            val sourceDir = pkg.applicationInfo.sourceDir
            val file = java.io.File(sourceDir)
            if (file.exists()) {
                // Heuristic: Many malware samples that use reflection or dynamic loading
                // will have certain strings visible even in a quick scan of the APK.
                val bytes = file.inputStream().use { it.readNBytes(1024 * 50) } // Read first 50KB
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

    private fun getNativeLibraries(pkg: PackageInfo): List<String> {
        val libs = mutableListOf<String>()
        try {
            val nativeLibDir = pkg.applicationInfo.nativeLibraryDir
            val dir = java.io.File(nativeLibDir)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".so")) {
                        libs.add(file.name)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return libs
    }

    private fun getSignature(pkg: PackageInfo): String {
        return try {
             val signatures = pkg.signatures
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
