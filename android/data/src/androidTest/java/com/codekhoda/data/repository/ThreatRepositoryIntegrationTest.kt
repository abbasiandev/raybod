package com.codekhoda.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.codekhoda.data.local.AppDatabase
import com.codekhoda.data.local.dao.RiskDao
import com.codekhoda.data.ml.MalwareScanner
import com.codekhoda.data.remote.api.CloudBrainApi
import com.codekhoda.domain.model.AppPackage
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.data.local.entity.SyncStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ThreatRepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var riskDao: RiskDao
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: CloudBrainApi
    private lateinit var malwareScanner: MalwareScanner
    private lateinit var repository: ThreatRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        riskDao = db.riskDao()

        mockWebServer = MockWebServer()
        mockWebServer.start()

        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudBrainApi::class.java)

        malwareScanner = mockk()
        repository = ThreatRepositoryImpl(riskDao, api, malwareScanner)
    }

    @After
    fun tearDown() {
        db.close()
        mockWebServer.shutdown()
    }

    @Test
    fun testScanApp_FullIntegration_Success() = runBlocking {
        // Given
        val app = AppPackage(
            packageName = "com.test.app", 
            versionCode = 1, 
            signature = "sig", 
            lastUpdateTime = 1000L,
            permissions = emptyList()
        )
        val localAssessment = RiskAssessment("com.test.app", RiskLevel.SAFE, "Local safe")
        
        coEvery { malwareScanner.scan(any()) } returns localAssessment
        
        // Mock Allowlist check (isAllowed = false to continue to analyzeApp)
        mockWebServer.enqueue(MockResponse().setBody("{\"packageName\": \"com.test.app\", \"isAllowed\": false}"))
        
        // Mock Cloud Analysis response
        val cloudJsonResponse = """
            {
                "packageName": "com.test.app",
                "riskLevel": "MEDIUM",
                "threatType": "Adware",
                "description": "Cloud found adware",
                "heuristicsUsed": ["PatternMatch"]
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(cloudJsonResponse))

        // When
        val result = repository.scanApp(app)

        // Then
        assertEquals(RiskLevel.MEDIUM, result.riskLevel)
        assertEquals("Adware", result.threatType)

        // Verify it's cached in DB with SYNCED status
        val cached = riskDao.getRisk("com.test.app")
        assertEquals("MEDIUM", cached?.riskLevel)
        assertEquals(SyncStatus.SYNCED.name, cached?.syncStatus)
    }

    @Test
    fun testScanApp_CloudFailure_FallbacksToLocalAndPending() = runBlocking {
        // Given
        val app = AppPackage(
            packageName = "com.test.app", 
            versionCode = 1, 
            signature = "sig", 
            lastUpdateTime = 1000L,
            permissions = emptyList()
        )
        val localAssessment = RiskAssessment("com.test.app", RiskLevel.SAFE, "Local safe")
        
        coEvery { malwareScanner.scan(any()) } returns localAssessment
        
        // Mock Allowlist failure
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        // Mock Analysis failure
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        // When
        val result = repository.scanApp(app)

        // Then
        assertEquals(RiskLevel.SAFE, result.riskLevel) // Should be local result

        // Verify it's cached in DB with PENDING status
        val cached = riskDao.getRisk("com.test.app")
        assertEquals("SAFE", cached?.riskLevel)
        assertEquals(SyncStatus.PENDING.name, cached?.syncStatus)
    }
}


