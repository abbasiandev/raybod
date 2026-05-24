package dev.abbasian.domain.model

/**
 * Represents an installed Android application with all extracted metadata.
 * Enhanced with context-aware fields for intelligent permission analysis.
 */
data class AppPackage(
    val packageName: String,
    val versionCode: Long,
    val versionName: String = "",
    val signature: String, // MD5 or SHA-256 hash of the signing certificate
    val permissions: List<String> = emptyList(),
    val intents: List<String> = emptyList(),
    val installTime: Long = 0,
    val lastUpdateTime: Long = 0,
    val nativeLibraries: List<String> = emptyList(),
    val activityCount: Int = 0,
    val serviceCount: Int = 0,
    val receiverCount: Int = 0,
    val hasReflection: Boolean = false,
    val hasDynamicLoading: Boolean = false,
    
    // Context-Aware Fields (Phase A)
    val appLabel: String = "",              // Human-readable app name
    val category: String = "",              // App category (e.g., "Navigation", "Photo", "Social")
    val installedSize: Long = 0,            // APK size in bytes
    val targetSdkVersion: Int = 0,          // Target SDK version
    val minSdkVersion: Int = 0              // Minimum SDK version
)
