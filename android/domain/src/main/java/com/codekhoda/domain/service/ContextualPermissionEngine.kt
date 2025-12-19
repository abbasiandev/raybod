package com.codekhoda.domain.service

/**
 * Context-Aware Permission Analysis Engine.
 * 
 * Implements the mentor's key insight: "Permissions alone don't tell the story—context is everything."
 * Maps app categories to expected permissions and adjusts risk scores accordingly.
 */
object ContextualPermissionEngine {

    /**
     * App categories for context-aware analysis.
     */
    enum class AppCategory(val keywords: List<String>) {
        NAVIGATION(listOf("map", "navigation", "gps", "waze", "uber", "lyft", "taxi", "drive")),
        PHOTO(listOf("camera", "photo", "gallery", "image", "pic", "selfie", "snap")),
        SOCIAL(listOf("social", "chat", "messenger", "whatsapp", "telegram", "instagram")),
        FINANCIAL(listOf("bank", "pay", "wallet", "finance", "money", "crypto", "payment")),
        UTILITY(listOf("flashlight", "calculator", "clock", "alarm", "weather", "note")),
        COMMUNICATION(listOf("phone", "call", "sms", "message", "dialer", "contact")),
        MEDIA(listOf("music", "video", "player", "stream", "spotify", "youtube")),
        HEALTH(listOf("health", "fitness", "medical", "heart", "step", "workout")),
        GAME(listOf("game", "play", "puzzle", "arcade", "adventure")),
        UNKNOWN(emptyList())
    }

    /**
     * Permissions expected for each app category.
     * If an app requests a permission NOT in its expected list, it's suspicious.
     */
    private val expectedPermissions: Map<AppCategory, Set<String>> = mapOf(
        AppCategory.NAVIGATION to setOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.INTERNET"
        ),
        AppCategory.PHOTO to setOf(
            "android.permission.CAMERA",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES"
        ),
        AppCategory.SOCIAL to setOf(
            "android.permission.INTERNET",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_EXTERNAL_STORAGE"
        ),
        AppCategory.FINANCIAL to setOf(
            "android.permission.INTERNET",
            "android.permission.USE_BIOMETRIC",
            "android.permission.USE_FINGERPRINT"
        ),
        AppCategory.UTILITY to setOf(
            "android.permission.CAMERA" // For flashlight control
        ),
        AppCategory.COMMUNICATION to setOf(
            "android.permission.CALL_PHONE",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_CONTACTS",
            "android.permission.SEND_SMS",
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS"
        ),
        AppCategory.MEDIA to setOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
        ),
        AppCategory.HEALTH to setOf(
            "android.permission.BODY_SENSORS",
            "android.permission.ACTIVITY_RECOGNITION",
            "android.permission.ACCESS_FINE_LOCATION"
        ),
        AppCategory.GAME to setOf(
            "android.permission.INTERNET",
            "android.permission.VIBRATE"
        ),
        AppCategory.UNKNOWN to emptySet()
    )

    /**
     * Permissions that are ALWAYS suspicious regardless of category.
     */
    private val alwaysSuspiciousPermissions = setOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.BIND_DEVICE_ADMIN",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.READ_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS"
    )

    /**
     * Infers app category from package name and app label.
     */
    fun inferCategory(packageName: String, appLabel: String, declaredCategory: String = ""): AppCategory {
        // First check declared category
        if (declaredCategory.isNotEmpty()) {
            AppCategory.values().find { 
                it.name.equals(declaredCategory, ignoreCase = true) 
            }?.let { return it }
        }

        // Infer from package name and label
        val searchText = "$packageName $appLabel".lowercase()
        
        return AppCategory.values()
            .filter { it.keywords.isNotEmpty() }
            .find { category -> 
                category.keywords.any { keyword -> searchText.contains(keyword) }
            } ?: AppCategory.UNKNOWN
    }

    /**
     * Calculates context-adjusted risk score.
     * 
     * @return Pair of (contextScore, unexpectedPermissions)
     *         contextScore: 0.0 (all expected) to 1.0 (highly suspicious)
     */
    fun calculateContextScore(
        category: AppCategory,
        permissions: List<String>
    ): ContextAnalysisResult {
        val expected = expectedPermissions[category] ?: emptySet()
        val unexpectedPermissions = mutableListOf<String>()
        val suspiciousPermissions = mutableListOf<String>()
        
        var score = 0f
        
        permissions.forEach { permission ->
            when {
                // Always suspicious permissions get highest weight
                alwaysSuspiciousPermissions.contains(permission) -> {
                    suspiciousPermissions.add(permission)
                    score += 0.3f
                }
                // Unexpected for category
                !expected.contains(permission) && isDangerousPermission(permission) -> {
                    unexpectedPermissions.add(permission)
                    score += 0.15f
                }
            }
        }
        
        // Unknown category gets slight penalty (can't verify context)
        if (category == AppCategory.UNKNOWN) {
            score += 0.1f
        }
        
        return ContextAnalysisResult(
            contextScore = score.coerceIn(0f, 1f),
            category = category,
            unexpectedPermissions = unexpectedPermissions,
            suspiciousPermissions = suspiciousPermissions,
            explanation = buildExplanation(category, unexpectedPermissions, suspiciousPermissions)
        )
    }

    /**
     * Checks if a permission is considered "dangerous" (requires runtime approval).
     */
    private fun isDangerousPermission(permission: String): Boolean {
        val dangerousGroups = listOf(
            "CAMERA", "RECORD_AUDIO", "READ_CONTACTS", "WRITE_CONTACTS",
            "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "ACCESS_BACKGROUND_LOCATION",
            "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "READ_MEDIA",
            "READ_SMS", "SEND_SMS", "RECEIVE_SMS", "READ_CALL_LOG",
            "READ_PHONE_STATE", "CALL_PHONE", "BODY_SENSORS"
        )
        return dangerousGroups.any { permission.contains(it, ignoreCase = true) }
    }

    private fun buildExplanation(
        category: AppCategory,
        unexpected: List<String>,
        suspicious: List<String>
    ): String {
        val parts = mutableListOf<String>()
        
        if (suspicious.isNotEmpty()) {
            parts.add("⚠️ ${suspicious.size} highly suspicious permissions")
        }
        if (unexpected.isNotEmpty() && category != AppCategory.UNKNOWN) {
            val shortNames = unexpected.map { it.substringAfterLast(".") }
            parts.add("Unexpected for ${category.name}: ${shortNames.joinToString()}")
        }
        
        return if (parts.isEmpty()) "Permissions match app context" else parts.joinToString("; ")
    }
}

/**
 * Result of context-aware permission analysis.
 */
data class ContextAnalysisResult(
    val contextScore: Float,           // 0.0 = all expected, 1.0 = highly suspicious
    val category: ContextualPermissionEngine.AppCategory,
    val unexpectedPermissions: List<String>,
    val suspiciousPermissions: List<String>,
    val explanation: String
)
