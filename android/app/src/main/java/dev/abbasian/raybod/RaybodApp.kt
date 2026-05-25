package dev.abbasian.raybod

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import dev.abbasian.data.ml.ModelUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class RaybodApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        SentryAndroid.init(this) { options ->
            options.dsn = "https://9f04fb087e015c61aef5f04dac069aa6@o4511446565715968.ingest.us.sentry.io/4511446567223296"
            options.environment = if (BuildConfig.DEBUG) "debug" else "production"
            options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            options.isDebug = BuildConfig.DEBUG
            options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.2
            options.isEnableAutoSessionTracking = true
            options.isAttachStacktrace = true
            options.isAttachThreads = true
        }
        super.onCreate()
        dev.abbasian.data.remote.BackendEndpoint.prefetchAsync()
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
