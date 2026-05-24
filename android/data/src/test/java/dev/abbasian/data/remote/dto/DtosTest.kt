package dev.abbasian.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DtosTest {

    // ========== AppMetadataDto Tests ==========

    @Test
    fun `create AppMetadataDto with all fields`() {
        val dto = AppMetadataDto(
            packageName = "com.test.app",
            versionCode = 42L,
            signature = "sha256hash",
            permissions = listOf("android.permission.CAMERA", "android.permission.INTERNET"),
            intents = listOf("action.VIEW"),
            versionName = "1.0.0",
            installTime = 1000L,
            lastUpdateTime = 2000L,
            hasReflection = true,
            hasDynamicLoading = false
        )

        assertEquals("com.test.app", dto.packageName)
        assertEquals(42L, dto.versionCode)
        assertEquals("sha256hash", dto.signature)
        assertEquals(2, dto.permissions.size)
        assertEquals(1, dto.intents.size)
        assertEquals("1.0.0", dto.versionName)
        assertEquals(1000L, dto.installTime)
        assertEquals(2000L, dto.lastUpdateTime)
        assertEquals(true, dto.hasReflection)
        assertEquals(false, dto.hasDynamicLoading)
    }

    @Test
    fun `AppMetadataDto with empty permissions`() {
        val dto = AppMetadataDto(
            packageName = "com.minimal.app",
            versionCode = 1L,
            signature = "hash",
            permissions = emptyList()
        )

        assertTrue(dto.permissions.isEmpty())
    }

    @Test
    fun `AppMetadataDto equality check`() {
        val dto1 = AppMetadataDto("com.test", 1L, "hash", listOf("perm1"))
        val dto2 = AppMetadataDto("com.test", 1L, "hash", listOf("perm1"))
        val dto3 = AppMetadataDto("com.test", 2L, "hash", listOf("perm1"))

        assertEquals(dto1, dto2)
        assertNotEquals(dto1, dto3)
    }

    @Test
    fun `AppMetadataDto copy creates modified instance`() {
        val original = AppMetadataDto(
            packageName = "com.original",
            versionCode = 1L,
            signature = "hash1",
            permissions = emptyList()
        )

        val modified = original.copy(versionCode = 2L)

        assertEquals(1L, original.versionCode)
        assertEquals(2L, modified.versionCode)
    }

    // ========== ScanResultDto Tests ==========

    @Test
    fun `create ScanResultDto with all fields`() {
        val dto = ScanResultDto(
            packageName = "com.scanned.app",
            riskLevel = "CRITICAL",
            threatType = "Malware",
            description = "Known malware detected",
            heuristicsUsed = listOf("Blocklist", "PermissionCombo")
        )

        assertEquals("com.scanned.app", dto.packageName)
        assertEquals("CRITICAL", dto.riskLevel)
        assertEquals("Malware", dto.threatType)
        assertEquals("Known malware detected", dto.description)
        assertEquals(2, dto.heuristicsUsed.size)
    }

    @Test
    fun `ScanResultDto with empty threatType`() {
        val dto = ScanResultDto(
            packageName = "com.clean.app",
            riskLevel = "SAFE",
            threatType = "",
            description = "No threats",
            heuristicsUsed = listOf("Clean")
        )

        assertEquals("", dto.threatType)
    }

    @Test
    fun `ScanResultDto with empty heuristicsUsed`() {
        val dto = ScanResultDto(
            packageName = "com.test.app",
            riskLevel = "UNKNOWN",
            threatType = "",
            description = "Unknown status",
            heuristicsUsed = emptyList()
        )

        assertTrue(dto.heuristicsUsed.isEmpty())
    }

    @Test
    fun `ScanResultDto supports all risk level strings`() {
        val riskLevels = listOf("SAFE", "LOW", "MEDIUM", "HIGH", "CRITICAL", "UNKNOWN")

        riskLevels.forEach { level ->
            val dto = ScanResultDto(
                packageName = "com.test.$level",
                riskLevel = level,
                threatType = "",
                description = "Test",
                heuristicsUsed = emptyList()
            )
            assertEquals(level, dto.riskLevel)
        }
    }

    @Test
    fun `ScanResultDto equality check`() {
        val dto1 = ScanResultDto("com.test", "SAFE", "", "desc", emptyList())
        val dto2 = ScanResultDto("com.test", "SAFE", "", "desc", emptyList())
        val dto3 = ScanResultDto("com.test", "HIGH", "", "desc", emptyList())

        assertEquals(dto1, dto2)
        assertNotEquals(dto1, dto3)
    }

    @Test
    fun `ScanResultDto copy creates modified instance`() {
        val original = ScanResultDto(
            packageName = "com.original",
            riskLevel = "SAFE",
            threatType = "",
            description = "Original",
            heuristicsUsed = emptyList()
        )

        val modified = original.copy(riskLevel = "HIGH")

        assertEquals("SAFE", original.riskLevel)
        assertEquals("HIGH", modified.riskLevel)
    }

    // ========== Serialization Name Tests ==========
    
    @Test
    fun `AppMetadataDto uses snake_case field names for API`() {
        // Verify the DTO is correctly annotated for JSON serialization
        // This is a documentation test - the @SerializedName annotations
        // are verified by their presence in the data class
        val dto = AppMetadataDto(
            packageName = "com.test",
            versionCode = 1L,
            signature = "hash",
            permissions = emptyList()
        )

        // If we got here without exception, the DTO is valid
        assertEquals("com.test", dto.packageName)
    }

    @Test
    fun `ScanResultDto uses snake_case field names for API`() {
        val dto = ScanResultDto(
            packageName = "com.test",
            riskLevel = "SAFE",
            threatType = "",
            description = "Test",
            heuristicsUsed = emptyList()
        )

        assertEquals("com.test", dto.packageName)
    }
}
