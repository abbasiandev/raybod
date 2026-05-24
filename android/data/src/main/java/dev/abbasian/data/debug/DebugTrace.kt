package dev.abbasian.data.debug

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object DebugTrace {
    private const val TAG = "RaybodDebug"
    private const val SESSION = "e7b765"
    private const val INGEST = "http://127.0.0.1:7249/ingest/fb28033a-9dae-4fbb-9c07-78ad45dfb7cf"
    private val client = OkHttpClient()

    fun log(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        runId: String = "post-fix"
    ) {
        val payload = JSONObject().apply {
            put("sessionId", SESSION)
            put("hypothesisId", hypothesisId)
            put("location", location)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
            put("runId", runId)
            put("data", JSONObject(data))
        }
        val line = payload.toString()
        Log.i(TAG, line)
        // #region agent log
        try {
            val body = line.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(INGEST)
                .addHeader("X-Debug-Session-Id", SESSION)
                .post(body)
                .build()
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.close()
                }
            })
        } catch (_: Exception) {
        }
        // #endregion
    }
}
