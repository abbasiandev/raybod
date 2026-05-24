package dev.abbasian.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/** Send the real backend hostname to the server/TLS layer when connecting by IP. */
class HostOverrideInterceptor(
    private val hostHeader: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Host", hostHeader)
            .build()
        return chain.proceed(request)
    }
}
