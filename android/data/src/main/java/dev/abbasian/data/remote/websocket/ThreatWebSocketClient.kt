package dev.abbasian.data.remote.websocket

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThreatWebSocketClient @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private val _threats = MutableSharedFlow<Map<String, Any>>(extraBufferCapacity = 1)
    val threats = _threats.asSharedFlow()

    fun connect() {
        if (webSocket != null) return

        val request = Request.Builder()
            .url("wss://gitr_g6pdx-727.b.jrnm.app/ws/threats")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = gson.fromJson<Map<String, Any>>(text, Map::class.java)
                    if (message["type"] == "NEW_THREAT") {
                        _threats.tryEmit(message)
                    }
                } catch (e: Exception) {
                    Log.e("ThreatWebSocket", "Error parsing message: $text", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                this@ThreatWebSocketClient.webSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ThreatWebSocket", "Connection failed", t)
                this@ThreatWebSocketClient.webSocket = null
                // In a real app, we'd implement exponential backoff here
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Service stopping")
        webSocket = null
    }
}



