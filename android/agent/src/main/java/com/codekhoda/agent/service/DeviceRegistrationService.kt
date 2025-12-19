package com.codekhoda.agent.service

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.codekhoda.data.remote.api.CloudBrainApi
import com.codekhoda.data.remote.dto.DeviceRegistrationDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRegistrationService @Inject constructor(
    private val api: CloudBrainApi
) {
    suspend fun registerDevice(context: Context) {
        try {
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appVersion = packageInfo.versionName ?: "unknown"
            
            val dto = DeviceRegistrationDto(
                deviceId = deviceId,
                deviceModel = Build.MODEL,
                osVersion = Build.VERSION.RELEASE,
                appVersion = appVersion
            )
            api.registerDevice(dto)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

