package dev.abbasian.agent.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.abbasian.agent.scanner.PackageAnalyzer
import dev.abbasian.domain.repository.ThreatRepository
import androidx.work.ListenableWorker.Result as WorkerResult

@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val threatRepository: ThreatRepository,
    private val packageAnalyzer: PackageAnalyzer
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): WorkerResult {
        val packageNames = workerParams.inputData
            .getString(CloudSyncScheduler.KEY_PACKAGE_NAMES)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        if (packageNames.isEmpty()) {
            Log.w(TAG, "No packages to sync")
            return WorkerResult.failure()
        }

        val appPackages = packageNames.mapNotNull { packageAnalyzer.analyzePackage(it) }
        if (appPackages.isEmpty()) {
            return WorkerResult.retry()
        }

        val synced = threatRepository.syncScanLogsToCloud(appPackages)
        Log.i(TAG, "Retry synced $synced/${appPackages.size} apps")

        return when {
            synced >= appPackages.size -> WorkerResult.success()
            synced > 0 -> WorkerResult.success()
            else -> WorkerResult.retry()
        }
    }

    private companion object {
        const val TAG = "RaybodCloud"
    }
}
