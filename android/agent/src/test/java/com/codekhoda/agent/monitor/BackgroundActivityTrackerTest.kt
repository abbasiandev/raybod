package com.codekhoda.agent.monitor

import com.codekhoda.domain.model.AccessType
import com.codekhoda.domain.model.PermissionUsageEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundActivityTrackerTest {

    @Test
    fun `getSuspiciousBackgroundActivities flags background microphone access`() {
        // Given
        val monitor = mockk<PermissionUsageMonitor>()
        val events = listOf(
            PermissionUsageEvent("com.spy.app", "perm", 1000L, wasInForeground = false, accessType = AccessType.MICROPHONE),
            PermissionUsageEvent("com.meeting.app", "perm", 1000L, wasInForeground = true, accessType = AccessType.MICROPHONE)
        )
        every { monitor.getRecentPermissionUsage(any()) } returns events
        
        val tracker = BackgroundActivityTracker(monitor)
        
        // When
        val suspicious = tracker.getSuspiciousBackgroundActivities()
        
        // Then
        assertEquals(1, suspicious.size)
        assertEquals("com.spy.app", suspicious[0].packageName)
    }

    @Test
    fun `getSuspiciousBackgroundActivities ignores non-sensitive background access`() {
        // Given
        val monitor = mockk<PermissionUsageMonitor>()
        val events = listOf(
            PermissionUsageEvent("com.weather.app", "perm", 1000L, wasInForeground = false, accessType = AccessType.UNKNOWN)
        )
        every { monitor.getRecentPermissionUsage(any()) } returns events
        
        val tracker = BackgroundActivityTracker(monitor)
        
        // When
        val suspicious = tracker.getSuspiciousBackgroundActivities()
        
        // Then
        assertEquals(0, suspicious.size)
    }
}
