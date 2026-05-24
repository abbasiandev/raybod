package dev.abbasian.agent.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dev.abbasian.agent.scanner.PackageAnalyzer
import dev.abbasian.domain.model.AppPackage
import dev.abbasian.domain.repository.ThreatRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CloudSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var threatRepository: ThreatRepository
    private lateinit var packageAnalyzer: PackageAnalyzer
    private lateinit var worker: CloudSyncWorker

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        threatRepository = mockk()
        packageAnalyzer = mockk()

        val inputData = Data.Builder()
            .putString(CloudSyncScheduler.KEY_PACKAGE_NAMES, "com.test.app,com.test.two")
            .build()
        workerParams = mockk(relaxed = true)
        every { workerParams.inputData } returns inputData

        worker = CloudSyncWorker(context, workerParams, threatRepository, packageAnalyzer)
    }

    @Test
    fun `doWork returns failure when no package names in input`() = runBlocking {
        every { workerParams.inputData } returns Data.EMPTY

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        coVerify(exactly = 0) { threatRepository.syncScanLogsToCloud(any()) }
    }

    @Test
    fun `doWork batch syncs packages successfully`() = runBlocking {
        val apps = listOf(
            AppPackage("com.test.app", 1, signature = "hash1"),
            AppPackage("com.test.two", 2, signature = "hash2")
        )
        coEvery { packageAnalyzer.analyzePackage("com.test.app") } returns apps[0]
        coEvery { packageAnalyzer.analyzePackage("com.test.two") } returns apps[1]
        coEvery { threatRepository.syncScanLogsToCloud(apps) } returns 2

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { threatRepository.syncScanLogsToCloud(apps) }
    }

    @Test
    fun `doWork retries when sync returns zero`() = runBlocking {
        val app = AppPackage("com.test.app", 1, signature = "hash1")
        coEvery { packageAnalyzer.analyzePackage("com.test.app") } returns app
        coEvery { packageAnalyzer.analyzePackage("com.test.two") } returns null
        coEvery { threatRepository.syncScanLogsToCloud(listOf(app)) } returns 0

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }
}
