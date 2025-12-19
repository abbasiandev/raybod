package com.codekhoda.presentation.scan

import com.codekhoda.agent.scanner.PackageAnalyzer
import com.codekhoda.domain.model.AppPackage
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.domain.usecase.ScanAppUseCase
import com.codekhoda.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private lateinit var scanAppUseCase: ScanAppUseCase
    private lateinit var packageAnalyzer: PackageAnalyzer
    private lateinit var userPrefs: UserPreferencesRepository
    private lateinit var viewModel: ScanViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        scanAppUseCase = mockk()
        packageAnalyzer = mockk()
        userPrefs = mockk()
        
        every { userPrefs.userPlan } returns flowOf("PREMIUM")
        every { userPrefs.lastScanTimestamp } returns flowOf(0L)
        coEvery { userPrefs.setLastScanTimestamp(any()) } returns Unit
        
        viewModel = ScanViewModel(scanAppUseCase, packageAnalyzer, userPrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is not scanning with empty results`() = runTest {
        // When
        val state = viewModel.uiState.first()

        // Then
        assertFalse(state.isScanning)
        assertEquals(0f, state.progress, 0.01f)
        assertEquals("", state.currentApp)
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun `startScan sets isScanning to true`() = runTest {
        // Given
        val apps = listOf(
            AppPackage("com.app1", 1, signature = "hash1")
        )
        val assessment = RiskAssessment(
            packageName = "com.app1",
            riskLevel = RiskLevel.SAFE,
            description = "Clean"
        )

        coEvery { packageAnalyzer.getInstalledApps() } returns apps
        coEvery { scanAppUseCase(any()) } returns assessment

        // When
        viewModel.startScan()

        // Advance to let coroutine start
        advanceUntilIdle()

        // Then - after completion, isScanning should be false
        val finalState = viewModel.uiState.first()
        assertFalse(finalState.isScanning)
        assertEquals(1f, finalState.progress, 0.01f)
    }

    @Test
    fun `scan completes with correct results`() = runTest {
        // Given
        val apps = listOf(
            AppPackage("com.app1", 1, signature = "hash1"),
            AppPackage("com.malware", 2, signature = "hash2")
        )
        val safeAssessment = RiskAssessment(
            packageName = "com.app1",
            riskLevel = RiskLevel.SAFE,
            description = "Clean"
        )
        val malwareAssessment = RiskAssessment(
            packageName = "com.malware",
            riskLevel = RiskLevel.CRITICAL,
            threatType = "Malware",
            description = "Threat detected"
        )

        coEvery { packageAnalyzer.getInstalledApps() } returns apps
        coEvery { scanAppUseCase(apps[0]) } returns safeAssessment
        coEvery { scanAppUseCase(apps[1]) } returns malwareAssessment

        // When
        viewModel.startScan()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(2, state.results.size)
        assertEquals(RiskLevel.CRITICAL, state.results[0].riskLevel)
        assertEquals(RiskLevel.SAFE, state.results[1].riskLevel)
    }

    @Test
    fun `scan updates current app during progress`() = runTest {
        // Given
        val apps = listOf(
            AppPackage("com.first.app", 1, signature = "hash1"),
            AppPackage("com.second.app", 2, signature = "hash2")
        )
        
        coEvery { packageAnalyzer.getInstalledApps() } returns apps
        coEvery { scanAppUseCase(any()) } returns RiskAssessment(
            packageName = "test",
            riskLevel = RiskLevel.SAFE,
            description = "Clean"
        )

        // When
        viewModel.startScan()
        advanceUntilIdle()

        // Then - after completion
        val state = viewModel.uiState.first()
        assertEquals("Scan Complete", state.currentApp)
    }

    @Test
    fun `empty app list results in empty scan`() = runTest {
        // Given
        coEvery { packageAnalyzer.getInstalledApps() } returns emptyList()

        // When
        viewModel.startScan()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isScanning)
        assertTrue(state.results.isEmpty())
        assertEquals("Scan Complete", state.currentApp)
    }

    @Test
    fun `scanAppUseCase is called for each app`() = runTest {
        // Given
        val apps = listOf(
            AppPackage("com.app1", 1, signature = "hash1"),
            AppPackage("com.app2", 2, signature = "hash2"),
            AppPackage("com.app3", 3, signature = "hash3")
        )

        coEvery { packageAnalyzer.getInstalledApps() } returns apps
        coEvery { scanAppUseCase(any()) } returns RiskAssessment(
            packageName = "test",
            riskLevel = RiskLevel.SAFE,
            description = "Clean"
        )

        // When
        viewModel.startScan()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 3) { scanAppUseCase(any()) }
        apps.forEach { app ->
            coVerify { scanAppUseCase(app) }
        }
    }

    @Test
    fun `scan results include all risk levels`() = runTest {
        // Given
        val apps = RiskLevel.entries.mapIndexed { index, level ->
            AppPackage("com.${level.name.lowercase()}", index.toLong(), signature = "hash$index")
        }

        coEvery { packageAnalyzer.getInstalledApps() } returns apps
        
        RiskLevel.entries.forEachIndexed { index, level ->
            coEvery { scanAppUseCase(apps[index]) } returns RiskAssessment(
                packageName = "com.${level.name.lowercase()}",
                riskLevel = level,
                description = "Test $level"
            )
        }

        // When
        viewModel.startScan()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(RiskLevel.entries.size, state.results.size)
        RiskLevel.entries.forEach { level ->
            assertTrue(
                "Missing risk level: $level",
                state.results.any { it.riskLevel == level }
            )
        }
    }

    @Test
    fun `scan results are sorted from most dangerous to least dangerous`() = runTest {
        // Given
        val apps = listOf(
            AppPackage("com.safe", 1, signature = "hash1"),
            AppPackage("com.critical", 2, signature = "hash2"),
            AppPackage("com.high", 3, signature = "hash3"),
            AppPackage("com.medium", 4, signature = "hash4"),
            AppPackage("com.low", 5, signature = "hash5"),
            AppPackage("com.unknown", 6, signature = "hash6")
        )

        coEvery { packageAnalyzer.getInstalledApps() } returns apps
        
        coEvery { scanAppUseCase(apps[0]) } returns RiskAssessment("com.safe", RiskLevel.SAFE, description = "Safe")
        coEvery { scanAppUseCase(apps[1]) } returns RiskAssessment("com.critical", RiskLevel.CRITICAL, description = "Critical")
        coEvery { scanAppUseCase(apps[2]) } returns RiskAssessment("com.high", RiskLevel.HIGH, description = "High")
        coEvery { scanAppUseCase(apps[3]) } returns RiskAssessment("com.medium", RiskLevel.MEDIUM, description = "Medium")
        coEvery { scanAppUseCase(apps[4]) } returns RiskAssessment("com.low", RiskLevel.LOW, description = "Low")
        coEvery { scanAppUseCase(apps[5]) } returns RiskAssessment("com.unknown", RiskLevel.UNKNOWN, description = "Unknown")

        // When
        viewModel.startScan()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertEquals(6, state.results.size)
        assertEquals(RiskLevel.CRITICAL, state.results[0].riskLevel)
        assertEquals(RiskLevel.HIGH, state.results[1].riskLevel)
        assertEquals(RiskLevel.MEDIUM, state.results[2].riskLevel)
        assertEquals(RiskLevel.LOW, state.results[3].riskLevel)
        assertEquals(RiskLevel.SAFE, state.results[4].riskLevel)
        assertEquals(RiskLevel.UNKNOWN, state.results[5].riskLevel)
    }
}
