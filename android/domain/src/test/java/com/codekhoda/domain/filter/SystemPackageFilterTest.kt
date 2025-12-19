package com.codekhoda.domain.filter

import org.junit.Assert.*
import org.junit.Test

class SystemPackageFilterTest {

    @Test
    fun `shouldExclude returns true for excluded packages`() {
        assertTrue(SystemPackageFilter.shouldExclude("com.google.android.apps.subscriptions.red"))
        assertTrue(SystemPackageFilter.shouldExclude("com.qualcomm.qti.qcolor"))
        assertTrue(SystemPackageFilter.shouldExclude("com.miui.face"))
        assertTrue(SystemPackageFilter.shouldExclude("com.facebook.appmanager"))
    }

    @Test
    fun `shouldExclude returns false for non-excluded packages`() {
        assertFalse(SystemPackageFilter.shouldExclude("com.example.myapp"))
        assertFalse(SystemPackageFilter.shouldExclude("com.custom.application"))
    }

    @Test
    fun `hasSuspiciousPermissions detects malicious permissions`() {
        val permissions = listOf(
            "android.permission.INTERNET",
            "com.qualcomm.qti.qcolor",
            "android.permission.READ_CONTACTS"
        )
        assertTrue(SystemPackageFilter.hasSuspiciousPermissions(permissions))
    }

    @Test
    fun `hasSuspiciousPermissions returns false for safe permissions`() {
        val permissions = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE"
        )
        assertFalse(SystemPackageFilter.hasSuspiciousPermissions(permissions))
    }

    @Test
    fun `filterPackages removes excluded packages`() {
        val packages = listOf(
            "com.example.myapp",
            "com.google.android.apps.subscriptions.red",
            "com.custom.app",
            "com.qualcomm.qti.qcolor",
            "com.another.app"
        )
        
        val filtered = SystemPackageFilter.filterPackages(packages)
        
        assertEquals(3, filtered.size)
        assertTrue(filtered.contains("com.example.myapp"))
        assertTrue(filtered.contains("com.custom.app"))
        assertTrue(filtered.contains("com.another.app"))
        assertFalse(filtered.contains("com.google.android.apps.subscriptions.red"))
        assertFalse(filtered.contains("com.qualcomm.qti.qcolor"))
    }

    @Test
    fun `all requested packages are in exclusion list`() {
        val requestedExclusions = listOf(
            "com.google.android.apps.subscriptions.red",
            "com.qualcomm.qti.qcolor",
            "com.qualcomm.qti.improvetouch",
            "com.miui.face",
            "com.android.cellbroadcastreceiver.overlay.common",
            "com.android.internal.systemui.navbar.gestural_narrow_back",
            "com.qualcomm.atfwd",
            "com.android.settings.resource.overlay",
            "com.qualcomm.timeservice",
            "com.qti.dcf",
            "com.miui.fmservice",
            "vendor.qti.imsdatachannel",
            "com.facebook.appmanager",
            "com.qualcomm.qti.poweroffalarm"
        )
        
        requestedExclusions.forEach { packageName ->
            assertTrue("$packageName should be excluded", SystemPackageFilter.shouldExclude(packageName))
        }
    }

    @Test
    fun `isSystemPackage detects Android system packages`() {
        assertTrue(SystemPackageFilter.isSystemPackage("com.android.systemui"))
        assertTrue(SystemPackageFilter.isSystemPackage("com.google.android.gms"))
        assertTrue(SystemPackageFilter.isSystemPackage("com.qualcomm.qcrilmsgtunnel"))
        assertTrue(SystemPackageFilter.isSystemPackage("com.miui.securitycenter"))
        assertTrue(SystemPackageFilter.isSystemPackage("com.samsung.android.app.settings"))
    }

    @Test
    fun `isSystemPackage returns false for user apps`() {
        assertFalse(SystemPackageFilter.isSystemPackage("com.whatsapp"))
        assertFalse(SystemPackageFilter.isSystemPackage("com.facebook.katana"))
        assertFalse(SystemPackageFilter.isSystemPackage("com.spotify.music"))
        assertFalse(SystemPackageFilter.isSystemPackage("com.example.myapp"))
    }

    @Test
    fun `shouldExclude uses prefix matching for system packages`() {
        assertTrue(SystemPackageFilter.shouldExclude("com.android.phone"))
        assertTrue(SystemPackageFilter.shouldExclude("com.google.android.apps.maps"))
        assertTrue(SystemPackageFilter.shouldExclude("com.samsung.android.messaging"))
        assertTrue(SystemPackageFilter.shouldExclude("com.huawei.systemmanager"))
        
        assertFalse(SystemPackageFilter.shouldExclude("com.instagram.android"))
        assertFalse(SystemPackageFilter.shouldExclude("com.twitter.android"))
    }

    @Test
    fun `common Android OS packages are properly identified`() {
        val commonSystemPackages = listOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.phone",
            "com.android.bluetooth",
            "com.android.nfc",
            "com.android.providers.contacts",
            "com.android.providers.calendar",
            "android.autoinstalls.config",
            "com.qualcomm.qcrilmsgtunnel",
            "com.qti.phone",
            "vendor.qti.hardware.data.connection",
            "com.miui.gallery",
            "com.xiaomi.market",
            "com.samsung.android.app.spage",
            "com.sec.android.app.launcher"
        )
        
        commonSystemPackages.forEach { packageName ->
            assertTrue(
                "$packageName should be identified as system package",
                SystemPackageFilter.isSystemPackage(packageName)
            )
        }
    }
}
