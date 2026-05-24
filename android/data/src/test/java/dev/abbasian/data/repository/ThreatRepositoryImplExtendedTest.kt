package dev.abbasian.data.repository

import dev.abbasian.data.local.dao.RiskDao
import dev.abbasian.data.ml.MalwareScanner
import dev.abbasian.data.remote.api.CloudBrainApi
import dev.abbasian.data.remote.dto.AllowlistCheckDto
import dev.abbasian.data.remote.dto.AppMetadataDto
import dev.abbasian.data.remote.dto.ScanResultDto
import dev.abbasian.domain.model.AppPackage
import dev.abbasian.domain.model.RiskAssessment
import dev.abbasian.domain.model.RiskLevel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThreatRepositoryImplExtendedTest {

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
    fun `sends extended metadata when available`() = runBlocking {
        // Given
        val appPackage = AppPackage(
            packageName = "com.test",
            versionCode = 1,
            versionName = "1.0.0",
            signature = "hash",
            permissions = listOf("CAMERA"),
            intents = listOf("android.intent.action.VIEW"),
            installTime = 1234567890L,
            lastUpdateTime = 1234567890L
        )
        
        val mockResult = ScanResultDto(
            packageName = "com.test",
            riskLevel = "SAFE",
            threatType = "",
            description = "Safe",
            heuristicsUsed = emptyList()
        )

        val capturedDto = slot<AppMetadataDto>()
        coEvery { api.checkAllowlist(any()) } returns AllowlistCheckDto("com.test", isAllowed = false)
        coEvery { riskDao.getRisk(any()) } returns null
        coEvery { malwareScanner.scan(appPackage) } returns RiskAssessment(
            packageName = "com.test",
            riskLevel = RiskLevel.SAFE,
            description = "Local scan"
        )
        coEvery { api.analyzeApp(capture(capturedDto)) } returns mockResult

        // When
        repository.scanApp(appPackage)

        // Then
        assertEquals(listOf("android.intent.action.VIEW"), capturedDto.captured.intents)
        assertEquals("1.0.0", capturedDto.captured.versionName)
        assertEquals(1234567890L, capturedDto.captured.installTime)
        assertEquals(1234567890L, capturedDto.captured.lastUpdateTime)
    }
}

