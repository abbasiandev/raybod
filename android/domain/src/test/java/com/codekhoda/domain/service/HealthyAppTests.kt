package com.codekhoda.domain.service

import com.codekhoda.domain.model.AppPackage
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive tests for legitimate/healthy apps to prevent false positives.
 * 
 * These tests ensure that popular, legitimate apps from the Play Store
 * are NOT incorrectly flagged as malware or suspicious.
 * 
 * Version 2.0 - Expanded with 20+ real-world app scenarios
 */
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
        
        assertEquals(ContextualPermissionEngine.AppCategory.MEDIA_PLAYER, category)
        
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
    
    @Test
    fun `Zoom is categorized as VIDEO_CALL and not flagged`() {
        val zoom = AppPackage(
            packageName = "us.zoom.videomeetings",
            versionCode = 500000,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.INTERNET",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
            ),
            appLabel = "Zoom",
            category = "Communication",
            installedSize = 80 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(zoom.packageName, zoom.appLabel)
        assertEquals(ContextualPermissionEngine.AppCategory.VIDEO_CALL, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, zoom.permissions)
        assertTrue("Zoom should have low context score: ${contextResult.contextScore}", contextResult.contextScore < 0.4f)
        assertFalse("Zoom should not be flagged as suspicious", contextResult.isSuspicious())
    }
    
    @Test
    fun `Microsoft Teams is not flagged as suspicious`() {
        val teams = AppPackage(
            packageName = "com.microsoft.teams",
            versionCode = 100000,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.INTERNET",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_EXTERNAL_STORAGE"
            ),
            appLabel = "Microsoft Teams",
            category = "Business",
            installedSize = 100 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(teams.packageName, teams.appLabel)
        assertEquals(ContextualPermissionEngine.AppCategory.VIDEO_CALL, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, teams.permissions)
        assertFalse("Teams should not be suspicious", contextResult.isSuspicious())
    }
    
    @Test
    fun `Dropbox is categorized as CLOUD_STORAGE and not flagged`() {
        val dropbox = AppPackage(
            packageName = "com.dropbox.android",
            versionCode = 200000,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.MANAGE_EXTERNAL_STORAGE",
                "android.permission.CAMERA"  // For document scanning
            ),
            appLabel = "Dropbox",
            category = "Productivity",
            installedSize = 60 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(dropbox.packageName, dropbox.appLabel)
        assertEquals(ContextualPermissionEngine.AppCategory.CLOUD_STORAGE, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, dropbox.permissions)
        assertTrue("Dropbox should have low context score", contextResult.contextScore < 0.3f)
    }
    
    @Test
    fun `Gmail is not flagged as suspicious`() {
        val gmail = AppPackage(
            packageName = "com.google.android.gm",
            versionCode = 123456,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.CAMERA",  // For attachments
                "android.permission.RECORD_AUDIO"  // For voice messages
            ),
            appLabel = "Gmail",
            category = "Communication",
            installedSize = 45 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(gmail.packageName, gmail.appLabel)
        assertEquals(ContextualPermissionEngine.AppCategory.EMAIL, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, gmail.permissions)
        assertFalse("Gmail should not be suspicious", contextResult.isSuspicious())
    }
    
    @Test
    fun `Telegram is not flagged as suspicious`() {
        val telegram = AppPackage(
            packageName = "org.telegram.messenger",
            versionCode = 300000,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.READ_CONTACTS",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.ACCESS_FINE_LOCATION"  // Location sharing
            ),
            appLabel = "Telegram",
            category = "Communication",
            installedSize = 55 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(telegram.packageName, telegram.appLabel)
        assertEquals(ContextualPermissionEngine.AppCategory.SOCIAL, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, telegram.permissions)
        assertFalse("Telegram should not be suspicious", contextResult.isSuspicious())
    }
    
    @Test
    fun `Simple Flashlight app is not flagged (small app)`() {
        val flashlight = AppPackage(
            packageName = "com.simple.flashlight",
            versionCode = 10,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.CAMERA",
                "android.permission.WAKE_LOCK"
            ),
            appLabel = "Flashlight",
            category = "Tools",
            installedSize = 1 * 1024 * 1024 // 1MB - legitimately small
        )
        
        val category = ContextualPermissionEngine.inferCategory(flashlight.packageName, flashlight.appLabel)
        assertEquals(ContextualPermissionEngine.AppCategory.UTILITY, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, flashlight.permissions)
        assertFalse("Flashlight should not be suspicious", contextResult.isSuspicious())
        
        val anomalies = SizePermissionAnalyzer.analyzeAnomalies(flashlight)
        assertTrue("Legitimate flashlight should have no anomalies: $anomalies", anomalies.isEmpty())
    }
    
    @Test
    fun `Firefox browser with camera for QR codes is not flagged`() {
        val firefox = AppPackage(
            packageName = "org.mozilla.firefox",
            versionCode = 100000,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.CAMERA",  // QR code scanning
                "android.permission.RECORD_AUDIO",  // Voice search
                "android.permission.ACCESS_FINE_LOCATION",  // Location services
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
            ),
            appLabel = "Firefox Browser",
            category = "Communication",
            installedSize = 70 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(firefox.packageName, firefox.appLabel)
        assertEquals(ContextualPermissionEngine.AppCategory.BROWSER, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, firefox.permissions)
        assertTrue("Firefox context score should be low: ${contextResult.contextScore}", contextResult.contextScore < 0.4f)
    }
    
    @Test
    fun `Instagram with camera and location is not flagged`() {
        val instagram = AppPackage(
            packageName = "com.instagram.android",
            versionCode = 200000,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",  // Stories
                "android.permission.READ_CONTACTS",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.ACCESS_FINE_LOCATION"  // Geo-tagging
            ),
            appLabel = "Instagram",
            category = "Social",
            installedSize = 90 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(instagram.packageName, instagram.appLabel)
        assertEquals(ContextualPermissionEngine.AppCategory.SOCIAL, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, instagram.permissions)
        assertFalse("Instagram should not be suspicious", contextResult.isSuspicious())
    }
    
    @Test
    fun `Adobe PDF Reader with camera for scanning is not flagged`() {
        val adobePdf = AppPackage(
            packageName = "com.adobe.reader",
            versionCode = 80000,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.CAMERA"  // Document scanning
            ),
            appLabel = "Adobe Acrobat Reader",
            category = "Productivity",
            installedSize = 55 * 1024 * 1024
        )
        
        val category = ContextualPermissionEngine.inferCategory(adobePdf.packageName, adobePdf.appLabel)
        assertEquals(ContextualPermissionEngine.AppCategory.PRODUCTIVITY, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, adobePdf.permissions)
        assertFalse("Adobe PDF should not be suspicious", contextResult.isSuspicious())
    }
    
    @Test
    fun `Pokemon GO with location and camera is not flagged (AR game)`() {
        val pokemonGo = AppPackage(
            packageName = "com.nianticlabs.pokemongo",
            versionCode = 150000,
            signature = "test_signature",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.CAMERA",  // AR mode
                "android.permission.ACCESS_FINE_LOCATION",  // Core gameplay
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.VIBRATE",
                "android.permission.READ_EXTERNAL_STORAGE"
            ),
            appLabel = "Pokémon GO",
            category = "Game",
            installedSize = 120 * 1024 * 1024
        )
        
        // Use declared category since package name doesn't contain "game"
        val category = ContextualPermissionEngine.inferCategory(pokemonGo.packageName, pokemonGo.appLabel, pokemonGo.category)
        assertEquals(ContextualPermissionEngine.AppCategory.GAME, category)
        
        val contextResult = ContextualPermissionEngine.calculateContextScore(category, pokemonGo.permissions)
        assertFalse("Pokemon GO should not be suspicious", contextResult.isSuspicious())
    }
}
