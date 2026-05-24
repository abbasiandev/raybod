package dev.abbasian.data.remote.interceptor

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.abbasian.data.debug.DebugTrace
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class DeviceIdInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val headerValue = deviceId ?: "unknown"
        if (chain.request().url.encodedPath.contains("scan/batch")) {
            // #region agent log
            DebugTrace.log(
                hypothesisId = "B",
                location = "DeviceIdInterceptor.kt:intercept",
                message = "Attaching device id to batch request",
                data = mapOf(
                    "deviceIdPresent" to (deviceId != null),
                    "deviceIdIsUnknown" to (headerValue == "unknown"),
                    "deviceIdPrefix" to headerValue.take(8)
                )
            )
            // #endregion
        }
        val request = chain.request().newBuilder()
            .header("X-Device-ID", headerValue)
            .build()
        return chain.proceed(request)
    }
}
