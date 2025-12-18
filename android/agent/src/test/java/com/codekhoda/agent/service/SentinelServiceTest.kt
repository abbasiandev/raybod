package com.codekhoda.agent.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.codekhoda.agent.scanner.PackageAnalyzer
import com.codekhoda.domain.model.AppPackage
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.domain.usecase.ScanAppUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SentinelServiceTest {

    private lateinit var scanAppUseCase: ScanAppUseCase
    private lateinit var packageAnalyzer: PackageAnalyzer
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        scanAppUseCase = mockk()
        packageAnalyzer = mockk()
    }

    @Test
    fun `onStartCommand starts foreground and scans specific package`() = runTest {
        // Given
        val packageName = "com.test.app"
        val appPackage = AppPackage(packageName, 1, signature = "sig")
        val assessment = RiskAssessment(packageName, RiskLevel.SAFE, "", "")
        
        coEvery { packageAnalyzer.analyzePackage(packageName) } returns appPackage
        coEvery { scanAppUseCase(appPackage) } returns assessment

        val intent = Intent(RuntimeEnvironment.getApplication(), SentinelService::class.java).apply {
            action = SentinelService.ACTION_SCAN_PACKAGE
            putExtra(SentinelService.EXTRA_PACKAGE_NAME, packageName)
        }

        // When
        val controller = Robolectric.buildService(SentinelService::class.java, intent)
        val service = controller.get()
        service.scanAppUseCase = scanAppUseCase
        service.packageAnalyzer = packageAnalyzer
        
        controller.create().startCommand(0, 0)

        // Then
        val shadowService = shadowOf(service)
        assert(shadowService.lastForegroundNotificationId != 0)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { packageAnalyzer.analyzePackage(packageName) }
        coVerify { scanAppUseCase(appPackage) }
        
        // Check notifications
        val notificationManager = RuntimeEnvironment.getApplication()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)
        
        // One foreground notification (ID 1) and one for progress/result (ID 2 or hash)
        assert(shadowNotificationManager.allNotifications.size >= 2)
    }

    @Test
    fun `onStartCommand scans all apps when no action specified`() = runTest {
        // Given
        val apps = listOf(
            AppPackage("com.app1", 1, signature = "sig1"),
            AppPackage("com.app2", 2, signature = "sig2")
        )
        coEvery { packageAnalyzer.getInstalledApps() } returns apps
        coEvery { scanAppUseCase(any()) } returns RiskAssessment("", RiskLevel.SAFE, "", "")

        // When
        val controller = Robolectric.buildService(SentinelService::class.java)
        val service = controller.get()
        service.scanAppUseCase = scanAppUseCase
        service.packageAnalyzer = packageAnalyzer
        
        controller.create().startCommand(0, 0)

        // Then
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { packageAnalyzer.getInstalledApps() }
        coVerify(exactly = 2) { scanAppUseCase(any()) }
    }
}
