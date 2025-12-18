package com.codekhoda.data.repository

import com.codekhoda.data.local.dao.RiskDao
import com.codekhoda.data.local.entity.CachedRiskEntity
import com.codekhoda.data.ml.MalwareScanner
import com.codekhoda.data.remote.api.CloudBrainApi
import com.codekhoda.data.remote.dto.AppMetadataDto
import com.codekhoda.data.remote.dto.ScanResultDto
import com.codekhoda.domain.model.AppPackage
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThreatRepositoryImplTest {

    private lateinit var riskDao: RiskDao
    private lateinit var api: CloudBrainApi
    private lateinit var malwareScanner: MalwareScanner
    private lateinit var repository: ThreatRepositoryImpl

    @Before
    fun setup() {
        riskDao = mockk(relaxed = true)
        api = mockk()
        malwareScanner = mockk()
        repository = ThreatRepositoryImpl(riskDao, api, malwareScanner)
    }

    @Test
    fun `returns cached result if available`() = runBlocking {
        // Given
        val packageName = "com.cached.app"
        val cachedEntity = CachedRiskEntity(
            packageName = packageName,
            riskLevel = "SAFE",
            threatType = "",
            description = "Cached result",
            timestamp = 1702900000000L
        )
        val appPackage = AppPackage(packageName, 1, signature = "hash")

        coEvery { riskDao.getRisk(packageName) } returns cachedEntity

        // When
        val result = repository.scanApp(appPackage)

        // Then
        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals("Cached result", result.description)
        coVerify(exactly = 0) { malwareScanner.scan(any()) }
        coVerify(exactly = 0) { api.analyzeApp(any()) }
    }

    @Test
    fun `returns local AI result immediately for CRITICAL threats`() = runBlocking {
        // Given
        val packageName = "com.malware.critical"
        val appPackage = AppPackage(
            packageName = packageName,
            versionCode = 1,
            signature = "malware_hash",
            permissions = listOf("android.permission.CAMERA")
        )
        val criticalAssessment = RiskAssessment(
            packageName = packageName,
            riskLevel = RiskLevel.CRITICAL,
            threatType = "Malware Detected",
            description = "AI Confidence: 95%"
        )

        coEvery { riskDao.getRisk(packageName) } returns null
        coEvery { malwareScanner.scan(appPackage) } returns criticalAssessment

        // When
        val result = repository.scanApp(appPackage)

        // Then
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
        coVerify(exactly = 0) { api.analyzeApp(any()) } // Cloud not called for CRITICAL
        coVerify { riskDao.insertRisk(any()) } // Result cached
    }

    @Test
    fun `calls cloud API for non-critical local results`() = runBlocking {
        // Given
        val packageName = "com.suspicious.app"
        val appPackage = AppPackage(
            packageName = packageName,
            versionCode = 1,
            signature = "hash",
            permissions = listOf("android.permission.CAMERA")
        )
        val localResult = RiskAssessment(
            packageName = packageName,
            riskLevel = RiskLevel.MEDIUM,
            description = "Local scan"
        )
        val cloudResponse = ScanResultDto(
            packageName = packageName,
            riskLevel = "HIGH",
            threatType = "Potential Spyware",
            description = "Cloud analysis",
            heuristicsUsed = listOf("PermissionCombo")
        )

        coEvery { riskDao.getRisk(packageName) } returns null
        coEvery { malwareScanner.scan(appPackage) } returns localResult
        coEvery { api.analyzeApp(any()) } returns cloudResponse

        // When
        val result = repository.scanApp(appPackage)

        // Then
        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertEquals("Potential Spyware", result.threatType)
        assertEquals("Cloud analysis", result.description)
        coVerify { api.analyzeApp(any()) }
    }

    @Test
    fun `falls back to local result when cloud API fails`() = runBlocking {
        // Given
        val packageName = "com.fallback.app"
        val appPackage = AppPackage(packageName, 1, signature = "hash")
        val localResult = RiskAssessment(
            packageName = packageName,
            riskLevel = RiskLevel.LOW,
            description = "Local fallback"
        )

        coEvery { riskDao.getRisk(packageName) } returns null
        coEvery { malwareScanner.scan(appPackage) } returns localResult
        coEvery { api.analyzeApp(any()) } throws Exception("Network error")

        // When
        val result = repository.scanApp(appPackage)

        // Then
        assertEquals(RiskLevel.LOW, result.riskLevel)
        assertEquals("Local fallback", result.description)
    }

    @Test
    fun `correctly maps AppPackage to API DTO`() = runBlocking {
        // Given
        val appPackage = AppPackage(
            packageName = "com.test.dto",
            versionCode = 42,
            signature = "test_signature",
            permissions = listOf("perm1", "perm2")
        )
        val localResult = RiskAssessment(
            packageName = "com.test.dto",
            riskLevel = RiskLevel.SAFE,
            description = "Safe"
        )
        val cloudResponse = ScanResultDto(
            packageName = "com.test.dto",
            riskLevel = "SAFE",
            threatType = "",
            description = "Clean",
            heuristicsUsed = emptyList()
        )

        val dtoSlot = slot<AppMetadataDto>()
        coEvery { riskDao.getRisk(any()) } returns null
        coEvery { malwareScanner.scan(any()) } returns localResult
        coEvery { api.analyzeApp(capture(dtoSlot)) } returns cloudResponse

        // When
        repository.scanApp(appPackage)

        // Then
        val capturedDto = dtoSlot.captured
        assertEquals("com.test.dto", capturedDto.packageName)
        assertEquals(42, capturedDto.versionCode)
        assertEquals("test_signature", capturedDto.signature)
        assertEquals(listOf("perm1", "perm2"), capturedDto.permissions)
    }

    @Test
    fun `scanApps processes multiple packages`() = runBlocking {
        // Given
        val packages = listOf(
            AppPackage("com.app1", 1, signature = "hash1"),
            AppPackage("com.app2", 2, signature = "hash2")
        )

        packages.forEach { pkg ->
            coEvery { riskDao.getRisk(pkg.packageName) } returns CachedRiskEntity(
                packageName = pkg.packageName,
                riskLevel = "SAFE",
                threatType = "",
                description = "Cached",
                timestamp = 0L
            )
        }

        // When
        val results = repository.scanApps(packages)

        // Then
        assertEquals(2, results.size)
        assertEquals("com.app1", results[0].packageName)
        assertEquals("com.app2", results[1].packageName)
    }

    @Test
    fun `handles invalid riskLevel from cloud gracefully`() = runBlocking {
        // Given
        val appPackage = AppPackage("com.invalid.level", 1, signature = "hash")
        val localResult = RiskAssessment(
            packageName = "com.invalid.level",
            riskLevel = RiskLevel.SAFE,
            description = "Local"
        )
        val cloudResponse = ScanResultDto(
            packageName = "com.invalid.level",
            riskLevel = "INVALID_LEVEL", // Invalid enum value
            threatType = "",
            description = "Cloud",
            heuristicsUsed = emptyList()
        )

        coEvery { riskDao.getRisk(any()) } returns null
        coEvery { malwareScanner.scan(any()) } returns localResult
        coEvery { api.analyzeApp(any()) } returns cloudResponse

        // When
        val result = repository.scanApp(appPackage)

        // Then
        assertEquals(RiskLevel.UNKNOWN, result.riskLevel)
    }
}
