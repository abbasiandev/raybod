package com.codekhoda.agent.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.codekhoda.data.local.dao.RiskDao
import com.codekhoda.data.local.entity.CachedRiskEntity
import com.codekhoda.data.local.entity.SyncStatus
import com.codekhoda.data.remote.api.CloudBrainApi
import com.codekhoda.data.remote.dto.ScanResultDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CloudSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var riskDao: RiskDao
    private lateinit var api: CloudBrainApi
    private lateinit var worker: CloudSyncWorker

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        riskDao = mockk(relaxed = true)
        api = mockk(relaxed = true)
        
        worker = CloudSyncWorker(context, workerParams, riskDao, api)
    }

    @Test
    fun `doWork returns success when no pending risks`() = runBlocking {
        coEvery { riskDao.getAllRisks() } returns emptyList()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { api.analyzeApp(any()) }
    }

    @Test
    fun `doWork syncs pending risks successfully`() = runBlocking {
        val pendingEntity = CachedRiskEntity(
            packageName = "com.test.app",
            riskLevel = "MEDIUM",
            appVersion = 100,
            threatType = "Test",
            description = "Test desc",
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        
        coEvery { riskDao.getAllRisks() } returns listOf(pendingEntity)
        coEvery { api.analyzeApp(any()) } returns ScanResultDto(
            packageName = "com.test.app",
            riskLevel = "MEDIUM",
            threatType = "Test",
            description = "Updated desc",
            heuristicsUsed = listOf("Cloud")
        )
        coEvery { riskDao.insertRisk(any()) } returns Unit

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { api.analyzeApp(any()) }
        coVerify { riskDao.insertRisk(any()) }
    }

    @Test
    fun `doWork retries on failure`() = runBlocking {
        val pendingEntity = CachedRiskEntity(
            packageName = "com.test.app",
            riskLevel = "MEDIUM",
            appVersion = 100,
            threatType = "Test",
            description = "Test desc",
            timestamp = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        
        coEvery { riskDao.getAllRisks() } returns listOf(pendingEntity)
        coEvery { api.analyzeApp(any()) } throws Exception("Network error")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
