package dev.abbasian.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0

        while (!response.isSuccessful && tryCount < maxRetries) {
            tryCount++
            // Close previous response body to avoid leak
            response.close()
            response = chain.proceed(request)
        }

        return response
    }
}



