package dev.abbasian.agent.service

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.abbasian.agent.scanner.PackageAnalyzer
import dev.abbasian.domain.model.RiskLevel
import dev.abbasian.domain.usecase.ScanAppUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SentinelService : Service() {

    companion object {
        const val ACTION_SCAN_PACKAGE = "dev.abbasian.agent.SCAN_PACKAGE"
        const val EXTRA_PACKAGE_NAME = "package_name"
        private const val CHANNEL_ID = "SentinelChannel"
        private const val ONGOING_NOTIFICATION_ID = 1
        private const val SCAN_RESULT_NOTIFICATION_ID = 2
    }

    @Inject
    lateinit var scanAppUseCase: ScanAppUseCase

    @Inject
    lateinit var packageAnalyzer: PackageAnalyzer

    @Inject
    lateinit var webSocketClient: dev.abbasian.data.remote.websocket.ThreatWebSocketClient

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var clipboardManager: ClipboardManager? = null
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        // Category 2.3: Clipboard Snooping Detection
        // Here we would check which app currently has focus or if the clipboard
        // contains sensitive patterns (crypto addresses, passwords) and 
        // if an unauthorized background app is accessing it.
        Log.d("SentinelService", "Clipboard content changed - monitoring for snooping")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupClipboardMonitor()
        SentinelFileObserver.startWatchingSensitiveAreas()
        
        webSocketClient.connect()
        observeGlobalThreats()
    }

    private fun observeGlobalThreats() {
        scope.launch {
            webSocketClient.threats.collect { threat ->
                val packageName = threat["package_name"] as? String ?: "Unknown"
                val riskLevelStr = threat["risk_level"] as? String ?: "HIGH"
                val riskLevel = try { RiskLevel.valueOf(riskLevelStr) } catch (e: Exception) { RiskLevel.HIGH }
                
                showResultNotification(
                    packageName = "[GLOBAL THREAT] $packageName",
                    riskLevel = riskLevel
                )
            }
        }
    }

    private fun setupClipboardMonitor() {
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                ONGOING_NOTIFICATION_ID, 
                createOngoingNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, createOngoingNotification())
        }

        if (intent?.action == ACTION_SCAN_PACKAGE) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            if (packageName != null) {
                scanSpecificPackage(packageName)
            }
        } else {
            // Initial scan of all apps if started normally
            scanAllApps()
        }

        return START_STICKY
    }

    private fun scanAllApps() {
        scope.launch {
            val apps = packageAnalyzer.getInstalledApps()
            apps.forEach { app ->
                scanAppUseCase(app)
            }
        }
    }

    private fun scanSpecificPackage(packageName: String) {
        scope.launch {
            showScanningNotification(packageName)
            val app = packageAnalyzer.analyzePackage(packageName)
            if (app != null) {
                val assessment = scanAppUseCase(app)
                showResultNotification(packageName, assessment.riskLevel)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sentinel Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Real-time malware protection"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createOngoingNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Raybod")
            .setContentText("Real-time protection active")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun showScanningNotification(packageName: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Scanning App")
            .setContentText("Analyzing $packageName...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(SCAN_RESULT_NOTIFICATION_ID, notification)
    }

    private fun showResultNotification(packageName: String, riskLevel: RiskLevel) {
        val (title, content, icon) = when (riskLevel) {
            RiskLevel.SAFE -> Triple("App Safe", "$packageName is clean", android.R.drawable.ic_dialog_info)
            RiskLevel.LOW -> Triple("Low Risk", "$packageName has minor concerns", android.R.drawable.ic_dialog_info)
            RiskLevel.MEDIUM -> Triple("Medium Risk", "$packageName requires attention", android.R.drawable.stat_notify_error)
            RiskLevel.HIGH, RiskLevel.CRITICAL -> Triple("Threat Detected!", "$packageName is risky!", android.R.drawable.ic_dialog_alert)
            RiskLevel.UNKNOWN -> Triple("Scan Complete", "Analysis finished for $packageName", android.R.drawable.ic_dialog_map)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(icon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(packageName.hashCode(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
        webSocketClient.disconnect()
        job.cancel()
    }
}
