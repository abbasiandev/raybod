package dev.abbasian.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // POST bodies are not safely replayed; retry only idempotent reads.
        if (request.method != "GET" && request.method != "HEAD") {
            return chain.proceed(request)
        }

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
        val NON_RETRYABLE_CODES = setOf(400, 401, 403, 404, 413, 422, 429, 500, 502, 503, 504)
    }
}
