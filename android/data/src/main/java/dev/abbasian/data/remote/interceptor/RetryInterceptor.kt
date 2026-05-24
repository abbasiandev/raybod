package dev.abbasian.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0

        while (!response.isSuccessful && tryCount < maxRetries && response.code !in NON_RETRYABLE_CODES) {
            tryCount++
            response.close()
            response = chain.proceed(request)
        }

        return response
    }

    private companion object {
        val NON_RETRYABLE_CODES = setOf(400, 401, 403, 404, 422, 429)
    }
}
