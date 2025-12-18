package com.codekhoda.agent.scanner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PackageAnalyzerTest {

    private lateinit var context: Context
    private lateinit var packageAnalyzer: PackageAnalyzer
    private lateinit var packageManager: PackageManager

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        packageAnalyzer = PackageAnalyzer(context)
        packageManager = context.packageManager
    }

    @Test
    fun `analyzePackage returns correct AppPackage info`() = runBlocking {
        // Given
        val packageName = "com.test.app"
        val packageInfo = PackageInfo().apply {
            this.packageName = packageName
            this.versionCode = 123
            this.versionName = "1.2.3"
            this.firstInstallTime = 1000L
            this.lastUpdateTime = 2000L
            this.requestedPermissions = arrayOf("android.permission.INTERNET")
            this.signatures = arrayOf(Signature(byteArrayOf(1, 2, 3)))
        }
        
        val shadowPackageManager = shadowOf(packageManager)
        shadowPackageManager.addPackage(packageInfo)

        // When
        val result = packageAnalyzer.analyzePackage(packageName)

        // Then
        assertNotNull(result)
        assertEquals(packageName, result?.packageName)
        assertEquals(123L, result?.versionCode)
        assertEquals("1.2.3", result?.versionName)
        assertEquals(listOf("android.permission.INTERNET"), result?.permissions)
        // Signature hash will be calculated, so we just check it's not empty
        assert(result?.signature?.isNotEmpty() == true)
    }

    @Test
    fun `getInstalledApps returns all installed packages`() = runBlocking {
        // Given
        val shadowPackageManager = shadowOf(packageManager)
        shadowPackageManager.addPackage(PackageInfo().apply { packageName = "app1"; signatures = arrayOf(Signature(byteArrayOf(1))) })
        shadowPackageManager.addPackage(PackageInfo().apply { packageName = "app2"; signatures = arrayOf(Signature(byteArrayOf(2))) })

        // When
        val result = packageAnalyzer.getInstalledApps()

        // Then
        // Robolectric might have pre-installed apps, so we check if ours are there
        val names = result.map { it.packageName }
        assert(names.contains("app1"))
        assert(names.contains("app2"))
    }
}
