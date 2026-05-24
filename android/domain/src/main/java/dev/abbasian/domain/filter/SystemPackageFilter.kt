package dev.abbasian.domain.filter

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
    private val excludedPackages = mutableSetOf(
        // Google services and subscriptions
        "com.google.android.gms",
        "com.android.vending",
        "com.google.android.apps.docs",
        "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.messaging",
        "com.google.android.gm",
        "com.google.android.networkstack",
        "com.google.android.apps.tips",
        "com.google.android.factoryota",
        "com.google.android.apps.maps",
        "com.google.android.keep",
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.subscriptions.red",
        
        // Trusted Communication & Productivity
        "org.thoughtcrime.securesms",
        "com.whatsapp",
        "com.skype.raider",
        "com.Slack",
        "jp.naver.line.android",
        "com.microsoft.office.outlook",
        "com.yahoo.mobile.client.android.mail",
        "com.dropbox.android",
        "com.pinterest",
        "com.termux",
        
        // System & Security Partners
        "org.jssec.android.activity.partneractivity",
        "org.jssec.android.activity.partneruser",
        
        // Samsung & Android System Components
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.android.chrome",
        "com.android.managedprovisioning",
        "com.android.vpndialogs",
        "com.android.internal.systemui.navbar.twobutton",
        "com.android.internal.systemui.navbar.threebutton",
        "com.android.cellbroadcastreceiver.overlay.common",
        "com.android.internal.systemui.navbar.gestural_narrow_back",
        "com.android.settings.resource.overlay",
        
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
        
        // Facebook system services
        "com.facebook.appmanager",
        "com.facebook.services"
    )

    /**
     * Update the excluded packages list from a remote source.
     */
    fun updateExcludedPackages(newPackages: Set<String>) {
        synchronized(excludedPackages) {
            excludedPackages.addAll(newPackages)
        }
    }

    /**
     * Known safe developer certificate signatures (SHA-256).
     * Used for Reputation Caching (Category 5.3).
     */
    private val trustedSignatures = setOf(
        "32a233bd3441634edc34305dc81775b63013db2367d383925c4ef9cefd25fe2b", // Example Google signature
        "029a28892410a56f6874ea9b00684618e38f9b9643f80879e658396c2a498263"  // Example Meta/FB signature
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
     * Check if a developer signature is trusted and should bypass deep scanning.
     */
    fun isTrustedSignature(signature: String): Boolean {
        return trustedSignatures.contains(signature.lowercase())
    }
    
    /**
     * Comprehensive set of dangerous permission combinations based on malware profiles (2024-2025).
     * Weights represent statistical confidence in maliciousness when these permissions are combined.
     * 
     * Key observations from 2024-2025 trends:
     * - Strategy Shift: Modern malware is becoming less "permission-heavy" to evade detection, 
     *   focusing instead on specific, high-impact synergic combinations [540, 2078].
     * - Accessibility Service: Remains a primary vector for Overlay Attacks when combined with 
     *   SYSTEM_ALERT_WINDOW, enabling credential theft via fake login screens [544, 588].
     * - Persistence: RECEIVE_BOOT_COMPLETED remains critical for ensuring malware survives 
     *   reboots, especially for Ransomware and Spyware [549, 550, 1731].
     * - Permission Gap: Discrepancies between declared and used permissions are strong 
     *   indicators of suspicious behavior [1228, 1342, 2392].
     */
    /**
     * Comprehensive set of dangerous permission combinations based on malware profiles (2024-2025).
     * Weights represent statistical confidence in maliciousness when these permissions are combined.
     * 
     * Key observations from 2024-2025 trends:
     * - Strategy Shift: Modern malware is becoming less "permission-heavy" to evade detection, 
     *   focusing instead on specific, high-impact synergic combinations [540, 2078].
     * - Accessibility Service: Remains a primary vector for Overlay Attacks when combined with 
     *   SYSTEM_ALERT_WINDOW, enabling credential theft via fake login screens [544, 588].
     * - Persistence: RECEIVE_BOOT_COMPLETED remains critical for ensuring malware survives 
     *   reboots, especially for Ransomware and Spyware [549, 550, 1731].
     * - Permission Gap: Discrepancies between declared and used permissions are strong 
     *   indicators of suspicious behavior [1228, 1342, 2392].
     */
    private val dangerousCombinations = listOf(
        // Advanced Spyware: Environment recording, camera, and continuous background tracking [550, 560]
        Triple(
            setOf(
                "android.permission.CAMERA", 
                "android.permission.RECORD_AUDIO", 
                "android.permission.ACCESS_BACKGROUND_LOCATION"
            ),
            0.98f,
            "Advanced Spyware (Camera/Audio/Background Location)"
        ),

        // ODF Banking Trojan: Credential theft via overlay and SMS interception [543, 546, 560]
        Triple(
            setOf(
                "android.permission.BIND_ACCESSIBILITY_SERVICE", 
                "android.permission.SYSTEM_ALERT_WINDOW", 
                "android.permission.READ_SMS", 
                "android.permission.INTERNET"
            ),
            0.97f,
            "ODF Banking Trojan (Overlay/SMS Theft)"
        ),

        // Ransomware (Screen Lock): Creating unbreakable screen and persistence after reboot [549, 560, 1212]
        Triple(
            setOf(
                "android.permission.SYSTEM_ALERT_WINDOW", 
                "android.permission.BIND_DEVICE_ADMIN", 
                "android.permission.RECEIVE_BOOT_COMPLETED"
            ),
            0.94f,
            "Screen-Lock Ransomware (Overlay/Persistence)"
        ),

        // Ransomware (Crypto): Full file access for encryption and C2 communication [547, 560]
        Triple(
            setOf(
                "android.permission.MANAGE_EXTERNAL_STORAGE", 
                "android.permission.INTERNET", 
                "android.permission.RECEIVE_BOOT_COMPLETED"
            ),
            0.90f,
            "Crypto Ransomware (Storage/Internet/Persistence)"
        ),

        // Privilege Scaler: Changing system security settings and interaction automation [555, 560]
        Triple(
            setOf(
                "android.permission.BIND_ACCESSIBILITY_SERVICE", 
                "android.permission.WRITE_SECURE_SETTINGS", 
                "android.permission.SYSTEM_ALERT_WINDOW"
            ),
            0.95f,
            "Privilege Scaler (Accessibility/Secure Settings)"
        ),

        // E2EE Interceptor: Bypassing end-to-end encryption via notifications [545, 552, 560]
        Triple(
            setOf(
                "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE", 
                "android.permission.INTERNET", 
                "android.permission.WAKE_LOCK"
            ),
            0.92f,
            "E2EE Interceptor (Notification Scraping)"
        ),

        // Location Stalker: Precise and stealthy background tracking with auto-start [550, 560]
        Triple(
            setOf(
                "android.permission.ACCESS_FINE_LOCATION", 
                "android.permission.ACCESS_BACKGROUND_LOCATION", 
                "android.permission.RECEIVE_BOOT_COMPLETED"
            ),
            0.93f,
            "Location Stalker (Background Tracking)"
        ),

        // Advanced Dropper: Security app identification and secondary malware installation [553, 560]
        Triple(
            setOf(
                "android.permission.REQUEST_INSTALL_PACKAGES", 
                "android.permission.QUERY_ALL_PACKAGES", 
                "android.permission.INTERNET"
            ),
            0.88f,
            "Advanced Dropper (Stealth Installation)"
        ),

        // Enterprise Spy: Data theft from work and personal profiles simultaneously [555, 560]
        Triple(
            setOf(
                "android.permission.INTERACT_ACROSS_USERS_FULL", 
                "android.permission.READ_CONTACTS", 
                "android.permission.INTERNET"
            ),
            0.85f,
            "Enterprise Spy (Cross-Profile Data Theft)"
        ),

        // Call Redirector: Monitoring phone state and redirecting outgoing calls [557, 560]
        Triple(
            setOf(
                "android.permission.PROCESS_OUTGOING_CALLS", 
                "android.permission.READ_PHONE_STATE", 
                "android.permission.CALL_PHONE"
            ),
            0.82f,
            "Call Redirector (Voice Communication Hijacking)"
        ),

        // Usage Monitor: Digital habit analysis and contact theft [551, 560]
        Triple(
            setOf(
                "android.permission.PACKAGE_USAGE_STATS", 
                "android.permission.READ_CONTACTS", 
                "android.permission.INTERNET"
            ),
            0.80f,
            "Usage Monitor (Behavioral Analysis)"
        ),

        // SMS Worm: Automated infection link spread to contact list [558, 560]
        Triple(
            setOf(
                "android.permission.READ_CONTACTS", 
                "android.permission.SEND_SMS"
            ),
            0.80f,
            "SMS Worm (Automated Propagation)"
        ),

        // Installer Rogue: App installation and removal (e.g., antivirus mitigation) [560, 2116]
        Triple(
            setOf(
                "android.permission.INSTALL_PACKAGES", 
                "android.permission.REQUEST_DELETE_PACKAGES", 
                "android.permission.QUERY_ALL_PACKAGES"
            ),
            0.84f,
            "Installer Rogue (Security Software Removal)"
        ),

        // App-Op Evader: Network state exploitation for post-reboot attack coordination [551, 560]
        Triple(
            setOf(
                "android.permission.ACCESS_NETWORK_STATE", 
                "android.permission.ACCESS_WIFI_STATE", 
                "android.permission.RECEIVE_BOOT_COMPLETED"
            ),
            0.70f,
            "App-Op Evader (Stealth Network Coordination)"
        ),

        // Notification Hijacker: 2FA code theft from notification bar [545, 560]
        Triple(
            setOf(
                "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE", 
                "android.permission.QUERY_ALL_PACKAGES"
            ),
            0.78f,
            "Notification Hijacker (2FA Interception)"
        )
    )

    /**
     * Check if a package has suspicious permissions.
     * 
     * @param permissions List of permissions requested by the package
     * @return true if any suspicious permissions are found, false otherwise
     */
    fun hasSuspiciousPermissions(permissions: List<String>): Boolean {
        // Individual suspicious permissions
        if (permissions.any { suspiciousPermissions.contains(it) }) return true
        
        // Use threshold for combinations
        return getPermissionRiskScore(permissions) >= 0.7f
    }

    /**
     * Calculate a combined risk score based on permission sets.
     * 
     * @param permissions List of requested permissions
     * @return Risk score between 0.0 and 1.0
     */
    fun getPermissionRiskScore(permissions: List<String>): Float {
        val requestedSet = permissions.toSet()
        var maxScore = 0f

        dangerousCombinations.forEach { (combo, score, _) ->
            if (requestedSet.containsAll(combo)) {
                if (score > maxScore) maxScore = score
            }
        }

        return maxScore
    }

    /**
     * Get descriptions of all matched dangerous permission combinations.
     * 
     * @param permissions List of requested permissions
     * @return List of matched combination descriptions
     */
    fun getMatchedPermissionCombinations(permissions: List<String>): List<String> {
        val requestedSet = permissions.toSet()
        val matches = mutableListOf<String>()

        dangerousCombinations.forEach { (combo, _, description) ->
            if (requestedSet.containsAll(combo)) {
                matches.add(description)
            }
        }

        return matches
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
