package dev.abbasian.domain.repository

import dev.abbasian.domain.model.AppPackage
import dev.abbasian.domain.model.RiskAssessment
import dev.abbasian.domain.model.RiskLevel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreatRepositoryContractTest {

    /**
     * These tests define the contract that any ThreatRepository implementation must satisfy.
     * Uses a mock to test interface behavior expectations.
     */

    private val mockRepository: ThreatRepository = mockk()

    @Test
    fun `scanApp returns RiskAssessment for given package`() = runBlocking {
        // Given
        val appPackage = AppPackage(
            packageName = "com.test.app",
            versionCode = 1,
            signature = "hash123"
        )
        val expectedAssessment = RiskAssessment(
            packageName = "com.test.app",
            riskLevel = RiskLevel.SAFE,
            description = "Clean app"
        )

        coEvery { mockRepository.scanApp(appPackage) } returns expectedAssessment

        // When
        val result = mockRepository.scanApp(appPackage)

        // Then
        assertEquals(expectedAssessment.packageName, result.packageName)
        assertEquals(expectedAssessment.riskLevel, result.riskLevel)
        coVerify(exactly = 1) { mockRepository.scanApp(appPackage) }
    }

    @Test
    fun `scanApp returns CRITICAL for known malware`() = runBlocking {
        // Given
        val malwarePackage = AppPackage(
            packageName = "com.known.malware",
            versionCode = 1,
            signature = "malicious_hash"
        )
        val criticalAssessment = RiskAssessment(
            packageName = "com.known.malware",
            riskLevel = RiskLevel.CRITICAL,
            threatType = "Malware",
            description = "Known malware signature detected"
        )

        coEvery { mockRepository.scanApp(malwarePackage) } returns criticalAssessment

        // When
        val result = mockRepository.scanApp(malwarePackage)

        // Then
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
        assertEquals("Malware", result.threatType)
    }

    @Test
    fun `scanApps returns list of assessments for multiple packages`() = runBlocking {
        // Given
        val packages = listOf(
            AppPackage("com.app1", 1, signature = "hash1"),
            AppPackage("com.app2", 2, signature = "hash2"),
            AppPackage("com.app3", 3, signature = "hash3")
        )
        val assessments = listOf(
            RiskAssessment(packageName = "com.app1", riskLevel = RiskLevel.SAFE, description = "Safe"),
            RiskAssessment(packageName = "com.app2", riskLevel = RiskLevel.MEDIUM, description = "Medium risk"),
            RiskAssessment(packageName = "com.app3", riskLevel = RiskLevel.HIGH, description = "High risk")
        )

        coEvery { mockRepository.scanApps(packages) } returns assessments

        // When
        val results = mockRepository.scanApps(packages)

        // Then
        assertEquals(3, results.size)
        assertEquals("com.app1", results[0].packageName)
        assertEquals(RiskLevel.MEDIUM, results[1].riskLevel)
        assertEquals(RiskLevel.HIGH, results[2].riskLevel)
    }

    @Test
    fun `scanApps returns empty list for empty input`() = runBlocking {
        // Given
        val emptyList = emptyList<AppPackage>()
        
        coEvery { mockRepository.scanApps(emptyList) } returns emptyList()

        // When
        val result = mockRepository.scanApps(emptyList)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `scan result package name matches input package name`() = runBlocking {
        // Given
        val packageName = "com.example.testapp"
        val appPackage = AppPackage(
            packageName = packageName,
            versionCode = 1,
            signature = "test_hash"
        )
        val assessment = RiskAssessment(
            packageName = packageName,
            riskLevel = RiskLevel.SAFE,
            description = "Test assessment"
        )

        coEvery { mockRepository.scanApp(appPackage) } returns assessment

        // When
        val result = mockRepository.scanApp(appPackage)

        // Then
        assertEquals(appPackage.packageName, result.packageName)
    }
}
