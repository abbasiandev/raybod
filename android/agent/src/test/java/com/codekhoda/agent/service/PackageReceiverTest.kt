package com.codekhoda.agent.service

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PackageReceiverTest {

    @Test
    fun `onReceive starts SentinelService for ACTION_PACKAGE_ADDED`() {
        // Given
        val context = RuntimeEnvironment.getApplication()
        val receiver = PackageReceiver()
        val packageName = "com.new.app"
        val intent = Intent(Intent.ACTION_PACKAGE_ADDED).apply {
            data = android.net.Uri.parse("package:$packageName")
        }

        // When
        receiver.onReceive(context, intent)

        // Then
        val shadowApp = shadowOf(context)
        val nextIntent = shadowApp.nextStartedService
        
        assertEquals(SentinelService::class.java.name, nextIntent.component?.className)
        assertEquals(SentinelService.ACTION_SCAN_PACKAGE, nextIntent.action)
        assertEquals(packageName, nextIntent.getStringExtra(SentinelService.EXTRA_PACKAGE_NAME))
    }

    @Test
    fun `onReceive starts SentinelService for ACTION_PACKAGE_REPLACED`() {
        // Given
        val context = RuntimeEnvironment.getApplication()
        val receiver = PackageReceiver()
        val packageName = "com.updated.app"
        val intent = Intent(Intent.ACTION_PACKAGE_REPLACED).apply {
            data = android.net.Uri.parse("package:$packageName")
        }

        // When
        receiver.onReceive(context, intent)

        // Then
        val shadowApp = shadowOf(context)
        val nextIntent = shadowApp.nextStartedService
        
        assertEquals(SentinelService::class.java.name, nextIntent.component?.className)
        assertEquals(SentinelService.ACTION_SCAN_PACKAGE, nextIntent.action)
        assertEquals(packageName, nextIntent.getStringExtra(SentinelService.EXTRA_PACKAGE_NAME))
    }
}
