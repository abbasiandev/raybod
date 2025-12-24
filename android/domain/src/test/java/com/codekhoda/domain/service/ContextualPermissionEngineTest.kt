package com.codekhoda.domain.service

import com.codekhoda.domain.service.ContextualPermissionEngine.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextualPermissionEngineTest {

    // ==========================================
    // Category Inference Tests
    // ==========================================

    @Test
    fun `inferCategory correctly identifies NAVIGATION apps`() {
        assertEquals(AppCategory.NAVIGATION, ContextualPermissionEngine.inferCategory("com.waze", "Waze"))
        assertEquals(AppCategory.NAVIGATION, ContextualPermissionEngine.inferCategory("com.google.maps", "Maps"))
        assertEquals(AppCategory.NAVIGATION, ContextualPermissionEngine.inferCategory("com.example.gps", "My GPS"))
    }

    @Test
    fun `inferCategory correctly identifies PHOTO apps`() {
        assertEquals(AppCategory.PHOTO, ContextualPermissionEngine.inferCategory("com.android.camera", "Camera"))
        assertEquals(AppCategory.PHOTO, ContextualPermissionEngine.inferCategory("com.google.photos", "Photos"))
        assertEquals(AppCategory.PHOTO, ContextualPermissionEngine.inferCategory("com.snapchat.android", "Snapchat")) // 'snap' keyword
    }

    @Test
    fun `inferCategory correctly identifies UTILITY apps`() {
        assertEquals(AppCategory.UTILITY, ContextualPermissionEngine.inferCategory("com.simple.flashlight", "Flashlight"))
        assertEquals(AppCategory.UTILITY, ContextualPermissionEngine.inferCategory("com.sec.android.app.clock", "Clock"))
    }

    @Test
    fun `inferCategory falls back to UNKNOWN for generic names`() {
        assertEquals(AppCategory.UNKNOWN, ContextualPermissionEngine.inferCategory("com.example.app", "My App"))
    }

    // ==========================================
    // Risk Scoring Tests
    // ==========================================

    @Test
    fun `Navigation app with Location is LOW risk`() {
        val permissions = listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.INTERNET"
        )
        val result = ContextualPermissionEngine.calculateContextScore(AppCategory.NAVIGATION, permissions)

        assertEquals(0.0f, result.contextScore, 0.01f)
        assertTrue(result.unexpectedPermissions.isEmpty())
        assertTrue(result.suspiciousPermissions.isEmpty())
    }

    @Test
    fun `Flashlight app with Location is SUSPICIOUS (Unexpected)`() {
        val permissions = listOf(
            "android.permission.CAMERA", // Expected
            "android.permission.ACCESS_FINE_LOCATION" // Unexpected
        )
        val result = ContextualPermissionEngine.calculateContextScore(AppCategory.UTILITY, permissions)

        // With new scoring: 0.18f for unexpected primary permission
        assertEquals(0.18f, result.contextScore, 0.01f)
        assertEquals(1, result.unexpectedPermissions.size)
        assertTrue(result.unexpectedPermissions.contains("android.permission.ACCESS_FINE_LOCATION"))
    }

    @Test
    fun `Flashlight app with SMS is HIGH RISK`() {
        val permissions = listOf(
            "android.permission.CAMERA",
            "android.permission.SEND_SMS", // Unexpected
            "android.permission.RECEIVE_SMS" // Unexpected
        )
        val result = ContextualPermissionEngine.calculateContextScore(AppCategory.UTILITY, permissions)

        // With new scoring: 0.18f * 2 = 0.36f (2 unexpected permissions)
        assertEquals(0.36f, result.contextScore, 0.01f)
        assertEquals(2, result.unexpectedPermissions.size)
    }

    @Test
    fun `App with Accessibility Service is ALWAYS marked suspicious`() {
        val permissions = listOf(
            "android.permission.BIND_ACCESSIBILITY_SERVICE"
        )
        // Even for a Utility app, this is suspicious if we don't strictly whitelist it
        val result = ContextualPermissionEngine.calculateContextScore(AppCategory.UTILITY, permissions)

        // New scoring: 0.35f (Highly Suspicious) since UTILITY is not in legitimateSuspiciousUseCases
        assertTrue("Score should be >= 0.35f, got ${result.contextScore}", result.contextScore >= 0.35f)
        assertTrue(result.suspiciousPermissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE"))
    }

    @Test
    fun `Unknown category app gets slight penalty`() {
        val permissions = listOf("android.permission.INTERNET")
        val result = ContextualPermissionEngine.calculateContextScore(AppCategory.UNKNOWN, permissions)

        // New scoring: 0.05f baseline for unknown (reduced from 0.1f)
        assertEquals(0.05f, result.contextScore, 0.01f)
    }

    @Test
    fun `Banking Trojan Profile - Flashlight requesting Admin + SMS`() {
        val permissions = listOf(
            "android.permission.CAMERA",
            "android.permission.BIND_DEVICE_ADMIN", // Highly Suspicious (+0.35)
            "android.permission.SEND_SMS" // Unexpected (+0.18)
        )
        val result = ContextualPermissionEngine.calculateContextScore(AppCategory.UTILITY, permissions)

        // New scoring: 0.35 (device admin) + 0.18 (SMS) = 0.53f (no multiple violations bonus since only 1 violation)
        assertTrue("Score should be >= 0.5f, got ${result.contextScore}", result.contextScore >= 0.5f)
        assertTrue("Should flag device admin", result.suspiciousPermissions.contains("android.permission.BIND_DEVICE_ADMIN"))
        assertTrue("Should flag SMS", result.unexpectedPermissions.contains("android.permission.SEND_SMS"))
        assertTrue("Should be critical", result.isCritical())
    }
}
