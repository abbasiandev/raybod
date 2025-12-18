package com.codekhoda.agent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val packageName = intent.data?.schemeSpecificPart ?: return

        if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
            val serviceIntent = Intent(context, SentinelService::class.java).apply {
                this.action = SentinelService.ACTION_SCAN_PACKAGE
                putExtra(SentinelService.EXTRA_PACKAGE_NAME, packageName)
            }
            context.startService(serviceIntent)
        } else if (action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, SentinelService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
