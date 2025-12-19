package com.codekhoda.domain.model

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
    val receiverCount: Int = 0
)
