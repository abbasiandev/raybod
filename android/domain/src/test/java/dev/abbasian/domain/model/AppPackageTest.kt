package dev.abbasian.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPackageTest {

    @Test
    fun `create AppPackage with required fields only`() {
        val appPackage = AppPackage(
            packageName = "com.example.app",
            versionCode = 1,
            signature = "abc123hash"
        )

        assertEquals("com.example.app", appPackage.packageName)
        assertEquals(1, appPackage.versionCode)
        assertEquals("abc123hash", appPackage.signature)
        assertEquals("", appPackage.versionName) // Default
        assertTrue(appPackage.permissions.isEmpty()) // Default
        assertTrue(appPackage.intents.isEmpty()) // Default
        assertEquals(0, appPackage.installTime) // Default
        assertEquals(0, appPackage.lastUpdateTime) // Default
        assertEquals(false, appPackage.hasReflection) // Default
        assertEquals(false, appPackage.hasDynamicLoading) // Default
    }

    @Test
    fun `create AppPackage with all fields`() {
        val permissions = listOf(
            "android.permission.CAMERA",
            "android.permission.INTERNET"
        )
        val intents = listOf(
            "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.VIEW"
        )
        
        val appPackage = AppPackage(
            packageName = "com.example.fullapp",
            versionCode = 42,
            versionName = "1.5.2",
            signature = "sha256hash",
            permissions = permissions,
            intents = intents,
            installTime = 1702900000000L,
            lastUpdateTime = 1702900100000L,
            activityCount = 10,
            serviceCount = 5,
            receiverCount = 2,
            hasReflection = true,
            hasDynamicLoading = true
        )

        assertEquals("com.example.fullapp", appPackage.packageName)
        assertEquals(42, appPackage.versionCode)
        assertEquals("1.5.2", appPackage.versionName)
        assertEquals("sha256hash", appPackage.signature)
        assertEquals(2, appPackage.permissions.size)
        assertEquals(2, appPackage.intents.size)
        assertEquals(1702900000000L, appPackage.installTime)
        assertEquals(1702900100000L, appPackage.lastUpdateTime)
        assertEquals(10, appPackage.activityCount)
        assertEquals(5, appPackage.serviceCount)
        assertEquals(2, appPackage.receiverCount)
        assertEquals(true, appPackage.hasReflection)
        assertEquals(true, appPackage.hasDynamicLoading)
    }

    @Test
    fun `AppPackage equality based on content`() {
        val app1 = AppPackage(
            packageName = "com.test",
            versionCode = 1,
            signature = "hash"
        )
        val app2 = AppPackage(
            packageName = "com.test",
            versionCode = 1,
            signature = "hash"
        )
        val app3 = AppPackage(
            packageName = "com.test",
            versionCode = 2, // Different version
            signature = "hash"
        )

        assertEquals(app1, app2)
        assertNotEquals(app1, app3)
    }

    @Test
    fun `copy function creates modified copy`() {
        val original = AppPackage(
            packageName = "com.original",
            versionCode = 1,
            signature = "hash1"
        )
        
        val modified = original.copy(versionCode = 2)

        assertEquals(1, original.versionCode)
        assertEquals(2, modified.versionCode)
        assertEquals(original.packageName, modified.packageName)
    }

    @Test
    fun `permissions list is immutable concern`() {
        val mutableList = mutableListOf("permission.ONE")
        val appPackage = AppPackage(
            packageName = "com.test",
            versionCode = 1,
            signature = "hash",
            permissions = mutableList
        )

        // Modifying original list shouldn't affect AppPackage if properly used
        assertEquals(1, appPackage.permissions.size)
    }
}
