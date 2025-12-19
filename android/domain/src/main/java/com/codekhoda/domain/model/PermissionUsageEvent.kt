package com.codekhoda.domain.model

/**
 * Represents a specific usage event of a permission by an app.
 * Used for runtime monitoring and audit logs.
 */
data class PermissionUsageEvent(
    val packageName: String,
    val permission: String,
    val timestamp: Long,
    val wasInForeground: Boolean,
    val durationMs: Long = 0, // 0 for instant access
    val accessType: AccessType = AccessType.UNKNOWN
)

enum class AccessType {
    CAMERA,
    MICROPHONE,
    LOCATION,
    CONTACTS,
    SMS,
    STORAGE,
    UNKNOWN
}
