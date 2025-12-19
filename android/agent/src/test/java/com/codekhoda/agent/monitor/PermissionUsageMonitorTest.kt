package com.codekhoda.agent.monitor

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import com.codekhoda.domain.model.AccessType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Constructor

class PermissionUsageMonitorTest {

    private lateinit var context: Context
    private lateinit var appOpsManager: AppOpsManager
    private lateinit var monitor: PermissionUsageMonitor

    @Before
    fun setup() {
        context = mockk()
        appOpsManager = mockk()
        every { context.getSystemService(AppOpsManager::class.java) } returns appOpsManager
        monitor = PermissionUsageMonitor(context)
    }

    @Test
    fun `getRecentPermissionUsage returns valid Camera event`() {
        // Given
        val now = System.currentTimeMillis()
        val opTime = now - 5000 // 5 seconds ago
        
        // Mocking OpEntry is tricky because it's final and sometimes tricky to mock final classes in pure unit tests 
        // without mockk-android or proper setup. 
        // However, we can try to mock it.
        val opEntry = mockk<AppOpsManager.OpEntry>()
        
        // Handling API differences in test? 
        // We can just mock the methods called by the class.
        // PermissionUsageMonitor uses opEntry.getLastAccessTime(flags) and opEntry.opStr
        
        // If SDK_INT >= 29 branch is taken in code (depending on environment of test runner)
        // Standard JUnit runs as "0" SDK version usually unless configured? 
        // Actually we might need to assume specific SDK or mock Build.VERSION.SDK_INT.
        // Setting Build.VERSION.SDK_INT is possible via reflection but easier to just mock both branches if needed
        // OR rely on the fact that we mocked the calls.
        
        every { opEntry.opStr } returns AppOpsManager.OPSTR_CAMERA
        // Mock deprecated method too just in case
        every { opEntry.time } returns opTime
        every { opEntry.duration } returns 1000
        
        // Mock new methods
        every { opEntry.getLastAccessTime(any()) } returns opTime
        every { opEntry.getLastDuration(any()) } returns 1000L
        
        val packageOps = mockk<AppOpsManager.PackageOps>()
        every { packageOps.packageName } returns "com.camera.app"
        every { packageOps.ops } returns listOf(opEntry)
        
        every { appOpsManager.getPackagesForOps(any()) } returns listOf(packageOps)
        
        // When
        val events = monitor.getRecentPermissionUsage()
        
        // Then
        assertEquals(1, events.size)
        val event = events[0]
        assertEquals("com.camera.app", event.packageName)
        assertEquals(AppOpsManager.OPSTR_CAMERA, event.permission)
        assertEquals(AccessType.CAMERA, event.accessType)
        assertEquals(opTime, event.timestamp)
    }

    @Test
    fun `getRecentPermissionUsage filters old events`() {
        // Given
        val now = System.currentTimeMillis()
        val opTime = now - 7200000 // 2 hours ago
        
        val opEntry = mockk<AppOpsManager.OpEntry>()
        every { opEntry.opStr } returns AppOpsManager.OPSTR_CAMERA
        every { opEntry.time } returns opTime
        every { opEntry.duration } returns 1000
        every { opEntry.getLastAccessTime(any()) } returns opTime
        every { opEntry.getLastDuration(any()) } returns 1000L
        
        val packageOps = mockk<AppOpsManager.PackageOps>()
        every { packageOps.packageName } returns "com.old.app"
        every { packageOps.ops } returns listOf(opEntry)
        
        every { appOpsManager.getPackagesForOps(any()) } returns listOf(packageOps)
        
        // When (duration 1 hour default)
        val events = monitor.getRecentPermissionUsage()
        
        // Then
        assertEquals(0, events.size)
    }
}
