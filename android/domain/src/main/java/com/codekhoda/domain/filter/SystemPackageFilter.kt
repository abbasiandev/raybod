package com.codekhoda.domain.filter

/**
 * Filter to exclude unnecessary system and OS packages from scanning.
 * These are typically pre-installed apps, overlays, and system services
 * that are part of the Android OS or device manufacturer's software.
 */
object SystemPackageFilter {
    
    /**
     * Package name prefixes that identify system/OS packages.
     * Any package starting with these prefixes will be considered a system package.
     */
    private val systemPackagePrefixes = listOf(
        "com.android.",
        "com.google.android.",
        "android.",
        "com.qualcomm.",
        "com.qti.",
        "vendor.qti.",
        "com.miui.",
        "com.xiaomi.",
        "com.samsung.",
        "com.sec.android.",
        "com.huawei.",
        "com.oppo.",
        "com.vivo.",
        "com.oneplus.",
        "com.motorola.",
        "com.lge.",
        "com.asus.",
        "com.sony.",
        "com.htc."
    )
    
    /**
     * List of specific package names to exclude from scanning.
     * These include system apps, manufacturer apps, and known safe packages.
     */
    private val excludedPackages = setOf(
        // Google services and subscriptions
        "com.google.android.apps.subscriptions.red",
        
        // Qualcomm system services
        "com.qualcomm.qti.qcolor",
        "com.qualcomm.qti.improvetouch",
        "com.qualcomm.atfwd",
        "com.qualcomm.timeservice",
        "com.qti.dcf",
        "vendor.qti.imsdatachannel",
        "com.qualcomm.qti.poweroffalarm",
        
        // MIUI system apps (Xiaomi)
        "com.miui.face",
        "com.miui.fmservice",
        
        // Android system components
        "com.android.cellbroadcastreceiver.overlay.common",
        "com.android.internal.systemui.navbar.gestural_narrow_back",
        "com.android.settings.resource.overlay",
        
        // Facebook system services (pre-installed on some devices)
        "com.facebook.appmanager"
    )
    
    /**
     * Patterns for permissions that should flag a package as suspicious.
     * These are used to detect potentially malicious packages even if they're not in the excluded list.
     */
    private val suspiciousPermissions = setOf(
        "com.qualcomm.qti.qcolor" // Flagged as malicious permission
    )
    
    /**
     * Check if a package should be excluded from scanning.
     * 
     * @param packageName The package name to check
     * @return true if the package should be excluded, false otherwise
     */
    fun shouldExclude(packageName: String): Boolean {
        // Check if it's in the explicit exclusion list
        if (excludedPackages.contains(packageName)) {
            return true
        }
        
        // Check if it matches any system package prefix
        return systemPackagePrefixes.any { prefix ->
            packageName.startsWith(prefix)
        }
    }
    
    /**
     * Check if a package is a system package based on prefix matching.
     * 
     * @param packageName The package name to check
     * @return true if the package is identified as a system package, false otherwise
     */
    fun isSystemPackage(packageName: String): Boolean {
        return systemPackagePrefixes.any { prefix ->
            packageName.startsWith(prefix)
        }
    }
    
    /**
     * Check if a package has suspicious permissions.
     * 
     * @param permissions List of permissions requested by the package
     * @return true if any suspicious permissions are found, false otherwise
     */
    fun hasSuspiciousPermissions(permissions: List<String>): Boolean {
        return permissions.any { permission ->
            suspiciousPermissions.contains(permission)
        }
    }
    
    /**
     * Filter a list of package names, removing excluded ones.
     * 
     * @param packages List of package names to filter
     * @return Filtered list with excluded packages removed
     */
    fun filterPackages(packages: List<String>): List<String> {
        return packages.filter { !shouldExclude(it) }
    }
}
