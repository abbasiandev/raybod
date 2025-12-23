package com.codekhoda.domain.service

import com.codekhoda.domain.model.AppPackage
import org.junit.Assert.*
import org.junit.Test

class HealthyAppTests {

    @Test
    fun `Google Chrome is not flagged as suspicious`() {
        val chrome = AppPackage(
            packageName = "com.android.chrome",
            versionCode = 123456789,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION"
            ),
            appLabel = "Chrome",
            category = "Productivity",
            installedSize = 50 * 1024 * 1024 // 50MB
        )
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(
            ContextualPermissionEngine.AppCategory.PRODUCTIVITY,
            chrome.permissions
        )
        
        assertTrue("Chrome should have low context score", contextResult.contextScore < 0.5f)
    }

    @Test
    fun `WhatsApp is not flagged as suspicious`() {
        val whatsapp = AppPackage(
            packageName = "com.whatsapp",
            versionCode = 456789,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_EXTERNAL_STORAGE"
            ),
            appLabel = "WhatsApp",
            category = "Social",
            installedSize = 60 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(
            whatsapp.packageName,
            whatsapp.appLabel,
            whatsapp.category
        )
        
        assertEquals(ContextualPermissionEngine.AppCategory.SOCIAL, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, whatsapp.permissions)
        assertTrue("WhatsApp should have low context score", contextResult.contextScore < 0.3f)
    }

    @Test
    fun `Google Maps is not flagged as suspicious`() {
        val maps = AppPackage(
            packageName = "com.google.android.apps.maps",
            versionCode = 987654,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_BACKGROUND_LOCATION",
                "android.permission.INTERNET"
            ),
            appLabel = "Maps",
            category = "Navigation",
            installedSize = 80 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(
            maps.packageName,
            maps.appLabel,
            maps.category
        )
        
        assertEquals(ContextualPermissionEngine.AppCategory.NAVIGATION, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, maps.permissions)
        assertEquals("Maps permissions should match expectations", 0, contextResult.unexpectedPermissions.size)
        assertTrue("Maps should have zero context score", contextResult.contextScore < 0.2f)
    }

    @Test
    fun `Xplore File Manager is categorized correctly`() {
        val xplore = AppPackage(
            packageName = "com.lonelycatgames.xplore",
            versionCode = 40100,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.MANAGE_EXTERNAL_STORAGE",
                "android.permission.INTERNET"
            ),
            appLabel = "X-plore File Manager",
            category = "",
            installedSize = 12 * 1024 * 1024 // 12MB
        )
        
        val category = ContextualPermissionEngine.inferCategory(
            xplore.packageName,
            xplore.appLabel,
            xplore.category
        )
        
        assertEquals("Xplore should be categorized as FILE_MANAGER", 
            ContextualPermissionEngine.AppCategory.FILE_MANAGER, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, xplore.permissions)
        assertTrue("Xplore should have low context score for file manager", contextResult.contextScore < 0.3f)
    }

    @Test
    fun `Camera app is not flagged as suspicious`() {
        val camera = AppPackage(
            packageName = "com.android.camera2",
            versionCode = 100,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.CAMERA",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION"
            ),
            appLabel = "Camera",
            category = "Photo",
            installedSize = 15 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(
            camera.packageName,
            camera.appLabel,
            camera.category
        )
        
        assertEquals(ContextualPermissionEngine.AppCategory.PHOTO, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, camera.permissions)
        // Camera apps may have location for geotagging, so score might be slightly higher
        assertTrue("Camera app should have acceptable context score: ${contextResult.contextScore}", 
            contextResult.contextScore < 0.5f)
    }

    @Test
    fun `Spotify is not flagged as suspicious`() {
        val spotify = AppPackage(
            packageName = "com.spotify.music",
            versionCode = 87654321,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
            ),
            appLabel = "Spotify",
            category = "Music",
            installedSize = 70 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(
            spotify.packageName,
            spotify.appLabel,
            spotify.category
        )
        
        assertEquals(ContextualPermissionEngine.AppCategory.MEDIA, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, spotify.permissions)
        assertTrue("Spotify should have low context score", contextResult.contextScore < 0.2f)
    }

    @Test
    fun `Size analyzer does not flag normal-sized apps`() {
        val normalApp = AppPackage(
            packageName = "com.example.normalapp",
            versionCode = 1,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.READ_EXTERNAL_STORAGE"
            ),
            appLabel = "Normal App",
            category = "Productivity",
            installedSize = 5 * 1024 * 1024 // 5MB
        )
        
        val anomalies = SizePermissionAnalyzer.analyzeAnomalies(normalApp)
        assertTrue("Normal app should have no size anomalies", anomalies.isEmpty())
    }

    @Test
    fun `Legitimate game with storage permissions is not flagged`() {
        val game = AppPackage(
            packageName = "com.example.legitgame",
            versionCode = 100,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.VIBRATE"
            ),
            appLabel = "Super Game",
            category = "Game",
            installedSize = 150 * 1024 * 1024 // 150MB
        )
        
        val category = ContextualPermissionEngine.inferCategory(
            game.packageName,
            game.appLabel,
            game.category
        )
        
        assertEquals(ContextualPermissionEngine.AppCategory.GAME, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, game.permissions)
        assertTrue("Game with storage should have low context score", contextResult.contextScore < 0.3f)
    }
}
