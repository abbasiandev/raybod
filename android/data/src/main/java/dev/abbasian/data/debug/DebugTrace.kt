package dev.abbasian.data.debug

import android.util.Log
import org.json.JSONObject

object DebugTrace {
    private const val TAG = "RaybodDebug"
    private const val SESSION = "e7b765"

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
    }
}
