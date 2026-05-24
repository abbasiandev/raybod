package dev.abbasian.agent.monitor

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import dev.abbasian.agent.service.PermissionOverlayService
import dev.abbasian.domain.model.AnomalyType
import dev.abbasian.domain.model.BehaviorAnomaly
import dev.abbasian.domain.service.BehaviorAnalysisEngine
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
    private lateinit var behaviorAnalysisEngine: BehaviorAnalysisEngine
    private lateinit var monitor: ActivePermissionMonitor

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        appOpsWrapper = mockk()
        behaviorAnalysisEngine = mockk(relaxed = true)
        monitor = ActivePermissionMonitor(context, appOpsWrapper, behaviorAnalysisEngine)
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
        assertEquals(false, intentSlot.captured.getBooleanExtra(PermissionOverlayService.EXTRA_SUSPICIOUS, true))
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

    @Test
    fun `checkAndNotify sets suspicious flag if behavior engine detects anomaly`() {
        // Given
        val activeOp = SimpleOpData(
            packageName = "com.malware.app",
            op = AppOpsManager.OPSTR_RECORD_AUDIO,
            timestamp = System.currentTimeMillis(),
            duration = 0,
            isForeground = false, // Background usage
            isRunning = true
        )
        every { appOpsWrapper.getRecentOps(any(), any()) } returns listOf(activeOp)

        val anomaly = BehaviorAnomaly(
            packageName = "com.malware.app",
            anomalyType = AnomalyType.UNEXPECTED_BACKGROUND_ACCESS,
            description = "Suspicious background audio",
            severity = 1.0f
        )
        every { behaviorAnalysisEngine.detectActivityMismatch(any(), any()) } returns listOf(anomaly)

        // When
        monitor.checkAndNotify()

        // Then
        val intentSlot = slot<Intent>()
        verify { context.startService(capture(intentSlot)) }
        
        assertEquals(PermissionOverlayService.ACTION_SHOW, intentSlot.captured.action)
        assertEquals(true, intentSlot.captured.getBooleanExtra(PermissionOverlayService.EXTRA_SUSPICIOUS, false))
    }
}
