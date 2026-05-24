package dev.abbasian.agent.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object CloudSyncScheduler {

    const val WORK_NAME = "cloud_scan_sync_retry"
    const val KEY_PACKAGE_NAMES = "package_names"

    fun enqueueRetry(context: Context, packageNames: List<String>) {
        if (packageNames.isEmpty()) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(KEY_PACKAGE_NAMES to packageNames.joinToString(",")))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
