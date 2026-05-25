package dev.abbasian.data.remote.interceptor

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class HostOverrideInterceptorTest {

    @Test
    fun `adds Host header for IP-based backend requests`() {
        val server = MockWebServer()
        server.enqueue(MockResponse())
        server.start()
        try {
            val client = OkHttpClient.Builder()
                .addInterceptor(HostOverrideInterceptor("gitr_g6pdx-727.b.jrnm.app"))
                .build()

            client.newCall(
                Request.Builder()
                    .url(server.url("/api/v1/scan"))
                    .build()
            ).execute().close()

            assertEquals("gitr_g6pdx-727.b.jrnm.app", server.takeRequest().getHeader("Host"))
        } finally {
            server.shutdown()
        }
    }
}
