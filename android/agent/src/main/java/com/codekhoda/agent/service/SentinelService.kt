package com.codekhoda.agent.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.codekhoda.agent.scanner.PackageAnalyzer
import com.codekhoda.domain.usecase.ScanAppUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SentinelService : Service() {

    @Inject
    lateinit var scanAppUseCase: ScanAppUseCase

    @Inject
    lateinit var packageAnalyzer: PackageAnalyzer

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        
        scope.launch {
            val apps = packageAnalyzer.getInstalledApps()
            apps.forEach { app ->
                // This will use On-Device AI first then Cloud if needed
                scanAppUseCase(app)
            }
        }
        
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "SentinelChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sentinel Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Hybrid Cloud Sentinel")
            .setContentText("Protecting your device...")
            .setSmallIcon(android.R.drawable.ic_secure) // Placeholder icon
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
