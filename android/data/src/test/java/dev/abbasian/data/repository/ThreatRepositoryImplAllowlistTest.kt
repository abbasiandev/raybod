package dev.abbasian.data.repository

import dev.abbasian.data.local.dao.RiskDao
import dev.abbasian.data.ml.MalwareScanner
import dev.abbasian.data.remote.api.CloudBrainApi
import dev.abbasian.data.remote.dto.AllowlistCheckDto
import dev.abbasian.domain.model.AppPackage
import dev.abbasian.domain.model.RiskAssessment
import dev.abbasian.domain.model.RiskLevel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ThreatRepositoryImplAllowlistTest {

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
    fun `returns safe immediately if package is in cloud allowlist`() = runBlocking {
        // Given
        val appPackage = AppPackage(
            packageName = "com.android.chrome",
            versionCode = 1,
            signature = "hash"
        )
        
        coEvery { api.checkAllowlist("com.android.chrome") } returns 
            AllowlistCheckDto("com.android.chrome", isAllowed = true)
        coEvery { riskDao.getRisk(any()) } returns null

        // When
        val result = repository.scanApp(appPackage, lowSpeedMode = false)

        // Then
        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertEquals("Verified Safe via Global Cloud Allowlist", result.description)
        coVerify(exactly = 0) { malwareScanner.scan(any()) }
        coVerify(exactly = 0) { api.analyzeApp(any()) }
    }
}

