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

@HiltWorker
class CloudSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val threatRepository: ThreatRepository,
    private val packageAnalyzer: PackageAnalyzer
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val packageNames = workerParams.inputData
            .getString(CloudSyncScheduler.KEY_PACKAGE_NAMES)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        if (packageNames.isEmpty()) {
            Log.w(TAG, "No packages to sync")
            return Result.failure()
        }

        val appPackages = packageNames.mapNotNull { packageAnalyzer.analyzePackage(it) }
        if (appPackages.isEmpty()) {
            return Result.retry()
        }

        val synced = threatRepository.syncScanLogsToCloud(appPackages)
        Log.i(TAG, "Retry synced $synced/${appPackages.size} apps")

        return when {
            synced >= appPackages.size -> Result.success()
            synced > 0 -> Result.success()
            else -> Result.retry()
        }
    }

    private companion object {
        const val TAG = "RaybodCloud"
    }
}
