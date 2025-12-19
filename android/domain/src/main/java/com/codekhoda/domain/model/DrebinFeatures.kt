package com.codekhoda.domain.model

/**
 * DREBIN Methodology: 8 sets of features extracted from APKs.
 * Reference: "DREBIN: Effective and Explainable Detection of Android Malware in Your Pocket"
 */
data class DrebinFeatures(
    val s1Hardware: List<String> = emptyList(),           // Hardware components
    val s2RequestedPermissions: List<String> = emptyList(), // Requested permissions
    val s3AppComponents: List<String> = emptyList(),      // Activities, Services, etc.
    val s4FilteredIntents: List<String> = emptyList(),    // Intents filtered by components
    val s5RestrictedApis: List<String> = emptyList(),     // Critical API calls in bytecode
    val s6UsedPermissions: List<String> = emptyList(),    // Permissions actually used in code
    val s7SuspiciousApis: List<String> = emptyList(),     // Non-protected but suspicious APIs
    val s8NetworkAddresses: List<String> = emptyList()    // URLs/IPs found in code
) {
    fun getAll(): List<String> = (s1Hardware + s2RequestedPermissions + s3AppComponents + 
                                  s4FilteredIntents + s5RestrictedApis + s6UsedPermissions + 
                                  s7SuspiciousApis + s8NetworkAddresses).distinct()
    
    fun isEmpty(): Boolean = s1Hardware.isEmpty() && s2RequestedPermissions.isEmpty() && 
                             s3AppComponents.isEmpty() && s4FilteredIntents.isEmpty() && 
                             s5RestrictedApis.isEmpty() && s6UsedPermissions.isEmpty() && 
                             s7SuspiciousApis.isEmpty() && s8NetworkAddresses.isEmpty()
}

