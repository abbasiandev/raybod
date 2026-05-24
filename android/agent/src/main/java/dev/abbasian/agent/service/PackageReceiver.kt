package dev.abbasian.agent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            startService(context)
            return
        }

        val packageName = intent.data?.schemeSpecificPart ?: return
        if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
            startService(context, packageName)
        }
    }

    private fun startService(context: Context, packageName: String? = null) {
        val serviceIntent = Intent(context, SentinelService::class.java).apply {
            if (packageName != null) {
                action = SentinelService.ACTION_SCAN_PACKAGE
                putExtra(SentinelService.EXTRA_PACKAGE_NAME, packageName)
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
