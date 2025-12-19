package com.codekhoda.agent.monitor

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import com.codekhoda.agent.service.PermissionOverlayService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActivePermissionMonitorTest {

    private lateinit var context: Context
    private lateinit var appOpsWrapper: AppOpsWrapper
    private lateinit var monitor: ActivePermissionMonitor

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        appOpsWrapper = mockk()
        monitor = ActivePermissionMonitor(context, appOpsWrapper)
    }

    @Test
    fun `checkAndNotify starts service with ACTION_SHOW if active op found`() {
        // Given
        val activeOp = SimpleOpData(
            packageName = "com.spy.app",
            op = AppOpsManager.OPSTR_CAMERA,
            timestamp = System.currentTimeMillis(),
            duration = 0,
            isForeground = true,
            isRunning = true
        )
        every { appOpsWrapper.getRecentOps(any(), any()) } returns listOf(activeOp)

        // When
        monitor.checkAndNotify()

        // Then
        val intentSlot = slot<Intent>()
        verify { context.startService(capture(intentSlot)) }
        
        assertEquals(PermissionOverlayService.ACTION_SHOW, intentSlot.captured.action)
        assertEquals("com.spy.app", intentSlot.captured.getStringExtra(PermissionOverlayService.EXTRA_PACKAGE))
        assertEquals(AppOpsManager.OPSTR_CAMERA, intentSlot.captured.getStringExtra(PermissionOverlayService.EXTRA_PERMISSION))
    }

    @Test
    fun `checkAndNotify starts service with ACTION_HIDE if no active op found`() {
        // Given
        val inactiveOp = SimpleOpData(
            packageName = "com.normal.app",
            op = AppOpsManager.OPSTR_CAMERA,
            timestamp = System.currentTimeMillis() - 5000,
            duration = 1000,
            isForeground = true,
            isRunning = false
        )
        every { appOpsWrapper.getRecentOps(any(), any()) } returns listOf(inactiveOp)

        // When
        monitor.checkAndNotify()

        // Then
        val intentSlot = slot<Intent>()
        verify { context.startService(capture(intentSlot)) }
        
        assertEquals(PermissionOverlayService.ACTION_HIDE, intentSlot.captured.action)
    }
}
