package com.codekhoda.domain.service

import com.codekhoda.domain.model.AnomalyType
import com.codekhoda.domain.model.PermissionUsageEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BehaviorAnalysisEngineTest {

    private val engine = BehaviorAnalysisEngine()

    @Test
    fun `detectActivityMismatch flags improper background camera usage`() {
        // Given: User is in a "Chat" activity (e.g., Messaging App)
        // But the same app is using Camera in the BACKGROUND (simulated by wasInForeground=false)
        // Note: Logic depends on how we define "Mismatch". 
        // For this test, we assume if currentActivity is NOT the package using permission, 
        // OR if the usage event says wasInForeground=false for a sensitive perm, it's an anomaly.
        
        val events = listOf(
            PermissionUsageEvent(
                packageName = "com.chat.app",
                permission = "android.permission.CAMERA",
                timestamp = System.currentTimeMillis(),
                wasInForeground = false, // Background usage!
                durationMs = 5000
            )
        )

        // When
        val anomalies = engine.detectActivityMismatch(
            currentForegroundApp = "com.other.app", // User is in another app
            recentPermissionEvents = events
        )

        // Then
        assertEquals(1, anomalies.size)
        val anomaly = anomalies.first()
        assertEquals("com.chat.app", anomaly.packageName)
        assertEquals(AnomalyType.UNEXPECTED_BACKGROUND_ACCESS, anomaly.anomalyType)
        assertTrue(anomaly.description.contains("CAMERA"))
    }

    @Test
    fun `detectActivityMismatch ignores safe foreground usage`() {
        // Given: User is in the app, and it uses camera
        val events = listOf(
            PermissionUsageEvent(
                packageName = "com.photo.app",
                permission = "android.permission.CAMERA",
                timestamp = System.currentTimeMillis(),
                wasInForeground = true, // Foreground usage
                durationMs = 5000
            )
        )

        // When
        val anomalies = engine.detectActivityMismatch(
            currentForegroundApp = "com.photo.app", 
            recentPermissionEvents = events
        )

        // Then
        assertTrue(anomalies.isEmpty())
    }

    @Test
    fun `detectActivityMismatch flags microphone usage when screen is off or app backgrounded`() {
        val events = listOf(
            PermissionUsageEvent(
                packageName = "com.spy.recorder",
                permission = "android.permission.RECORD_AUDIO",
                timestamp = System.currentTimeMillis(),
                wasInForeground = false,
                durationMs = 10000
            )
        )

        val anomalies = engine.detectActivityMismatch(
            currentForegroundApp = "com.launcher",
            recentPermissionEvents = events
        )

        assertEquals(1, anomalies.size)
        assertEquals(AnomalyType.UNEXPECTED_BACKGROUND_ACCESS, anomalies.first().anomalyType)
    }
}
