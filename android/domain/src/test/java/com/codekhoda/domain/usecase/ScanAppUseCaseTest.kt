package com.codekhoda.domain.usecase

import com.codekhoda.domain.model.AppPackage
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.domain.repository.ThreatRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanAppUseCaseTest {

    private val threatRepository: ThreatRepository = mockk()
    private val scanAppUseCase = ScanAppUseCase(threatRepository)

    @Test
    fun `invoke calls repository and returns risk assessment`() = runBlocking {
        // Given
        val appPackage = AppPackage(
            packageName = "com.malware.test",
            versionCode = 1,
            signature = "hash123",
            permissions = listOf("android.permission.CAMERA")
        )
        val expectedAssessment = RiskAssessment(
            packageName = "com.malware.test",
            riskLevel = RiskLevel.HIGH,
            description = "Suspicious behavior detected"
        )
        
        coEvery { threatRepository.scanApp(appPackage) } returns expectedAssessment

        // When
        val result = scanAppUseCase(appPackage)

        // Then
        assertEquals(expectedAssessment, result)
        coVerify(exactly = 1) { threatRepository.scanApp(appPackage) }
    }
}
