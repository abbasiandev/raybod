package dev.abbasian.agent.monitor

import android.app.AppOpsManager
import dev.abbasian.domain.model.AccessType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PermissionUsageMonitorTest {

    private lateinit var appOpsWrapper: AppOpsWrapper
    private lateinit var monitor: PermissionUsageMonitor

    @Before
    fun setup() {
        appOpsWrapper = mockk()
        monitor = PermissionUsageMonitor(appOpsWrapper)
    }

    @Test
    fun `getRecentPermissionUsage maps OpData to Event correctly`() {
        // Given
        val now = System.currentTimeMillis()
        val opTime = now - 5000 // 5 seconds ago
        
        val opData = SimpleOpData(
            packageName = "com.camera.app",
            op = AppOpsManager.OPSTR_CAMERA,
            timestamp = opTime,
            duration = 1000L,
            isForeground = true
        )
        
        every { appOpsWrapper.getRecentOps(any(), any()) } returns listOf(opData)
        
        // When
        val events = monitor.getRecentPermissionUsage()
        
        // Then
        assertEquals(1, events.size)
        val event = events[0]
        assertEquals("com.camera.app", event.packageName)
        assertEquals(AppOpsManager.OPSTR_CAMERA, event.permission)
        assertEquals(AccessType.CAMERA, event.accessType)
        assertEquals(opTime, event.timestamp)
        assertEquals(true, event.wasInForeground)
    }

    @Test
    fun `getRecentPermissionUsage returns empty if no ops found`() {
        // Given
        every { appOpsWrapper.getRecentOps(any(), any()) } returns emptyList()
        
        // When
        val events = monitor.getRecentPermissionUsage()
        
        // Then
        assertEquals(0, events.size)
    }
}
