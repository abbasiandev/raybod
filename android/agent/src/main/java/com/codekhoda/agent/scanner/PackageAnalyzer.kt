package com.codekhoda.agent.scanner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.codekhoda.domain.model.AppPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class PackageAnalyzer(private val context: Context) {

    suspend fun getInstalledApps(): List<AppPackage> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES)
        
        packages.map { pkg ->
            convertPackageInfo(pkg)
        }
    }

    suspend fun analyzePackage(packageName: String): AppPackage? = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val pkg = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES)
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
        
        return AppPackage(
            packageName = pkg.packageName,
            versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) pkg.longVersionCode else pkg.versionCode.toLong(),
            versionName = pkg.versionName ?: "",
            signature = signature,
            permissions = permissions,
            intents = intents,
            installTime = pkg.firstInstallTime,
            lastUpdateTime = pkg.lastUpdateTime
        )
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
