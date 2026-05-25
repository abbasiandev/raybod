package dev.abbasian.data.remote

import android.os.Looper
import android.util.Log

/**
 * JustRunMy hostnames contain underscores, which Android Java networking rejects (IDN.toASCII).
 * Connect via IPv4 + Host header instead. Never block the main thread with DNS/HTTP lookups.
 */
object BackendEndpoint {
    const val HOST = "gitr_g6pdx-727.b.jrnm.app"

    /** Last known good IP for this deployment; used when DoH is unavailable at startup. */
    private const val FALLBACK_IPV4 = "46.224.69.39"
    private const val TAG = "RaybodCloud"

    @Volatile
    private var cachedIpv4: String? = null

    @Volatile
    private var prefetchStarted = false

    fun prefetchAsync() {
        if (cachedIpv4 != null) return
        synchronized(this) {
            if (prefetchStarted || cachedIpv4 != null) return
            prefetchStarted = true
            Thread({
                try {
                    resolveAndCache()
                } catch (e: Exception) {
                    Log.w(TAG, "Background DNS prefetch failed: ${e.message}")
                }
            }, "backend-dns-prefetch").start()
        }
    }

    fun ipv4(): String {
        cachedIpv4?.let { return it }
        synchronized(this) {
            cachedIpv4?.let { return it }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Log.w(TAG, "Using fallback IP on main thread; prefetching real IP in background")
                prefetchAsync()
                return FALLBACK_IPV4
            }
            return resolveAndCache()
        }
    }

    private fun resolveAndCache(): String {
        val ip = try {
            NetworkDns.lookupPublicIpv4(HOST).firstOrNull()?.hostAddress?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Public DNS lookup failed for $HOST: ${e.message}")
            null
        } ?: FALLBACK_IPV4

        cachedIpv4 = ip
        Log.i(TAG, "Backend endpoint $HOST -> $ip")
        return ip
    }

    fun httpsBaseUrl(): String = "https://${ipv4()}/"

    fun wssThreatsUrl(): String = "wss://${ipv4()}/ws/threats"
}
