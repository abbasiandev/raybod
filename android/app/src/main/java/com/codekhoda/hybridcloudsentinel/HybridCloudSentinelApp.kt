package com.codekhoda.hybridcloudsentinel

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.codekhoda.data.ml.ModelUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class HybridCloudSentinelApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleModelUpdates()
    }

    private fun scheduleModelUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val modelUpdateRequest = PeriodicWorkRequestBuilder<ModelUpdateWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .addTag("model_update")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ModelUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            modelUpdateRequest
        )
    }
}
