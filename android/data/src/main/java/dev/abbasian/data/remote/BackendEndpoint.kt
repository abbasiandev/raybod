package dev.abbasian.data.remote

import android.util.Log

/**
 * JustRunMy hostnames contain underscores, which Android Java networking rejects (IDN.toASCII).
 * Resolve the backend to an IPv4 address once and connect via IP + Host header instead.
 */
object BackendEndpoint {
    const val HOST = "gitr_g6pdx-727.b.jrnm.app"
    private const val TAG = "RaybodCloud"

    @Volatile
    private var cachedIpv4: String? = null

    fun ipv4(): String {
        cachedIpv4?.let { return it }
        synchronized(this) {
            cachedIpv4?.let { return it }
            val addresses = NetworkDns.lookupPublicIpv4(HOST)
            val ip = addresses.firstOrNull()?.hostAddress
                ?: throw IllegalStateException("Could not resolve backend host: $HOST")
            cachedIpv4 = ip
            Log.i(TAG, "Backend endpoint $HOST -> $ip")
            return ip
        }
    }

    fun httpsBaseUrl(): String = "https://${ipv4()}/"

    fun wssThreatsUrl(): String = "wss://${ipv4()}/ws/threats"
}
