package dev.abbasian.data.remote.interceptor

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class DeviceIdInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val request = chain.request().newBuilder()
            .header("X-Device-ID", deviceId ?: "unknown")
            .build()
        return chain.proceed(request)
    }
}
