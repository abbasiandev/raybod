package dev.abbasian.data.remote

import android.os.Looper
import android.util.Log
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object NetworkDns {
    private const val TAG = "RaybodCloud"
    private val IP_LITERAL = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

    private val systemDns: Dns = Dns.SYSTEM
    private val bootstrapClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val preferIpv4: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            if (IP_LITERAL.matches(hostname)) {
                return listOf(InetAddress.getByName(hostname))
            }
            if (hostname.contains('_')) {
                assertBackgroundThread("underscore hostname DNS")
                return lookupPublicIpv4(hostname)
            }
            return preferIpv4OrAll(systemDns.lookup(hostname))
        }
    }

    fun lookupPublicIpv4(hostname: String): List<InetAddress> {
        assertBackgroundThread("public DNS lookup")
        Log.i(TAG, "Public DNS lookup for: $hostname")
        return try {
            queryGoogleDns(hostname)
        } catch (e: Exception) {
            Log.w(TAG, "Google DNS failed for $hostname: ${e.message}")
            throw UnknownHostException("$hostname (${e.message})")
        }
    }

    private fun queryGoogleDns(hostname: String): List<InetAddress> {
        val encoded = URLEncoder.encode(hostname, Charsets.UTF_8.name())
        val request = Request.Builder()
            .url("https://dns.google/resolve?name=$encoded&type=A")
            .header("Accept", "application/dns-json")
            .build()

        bootstrapClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw UnknownHostException("$hostname HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw UnknownHostException(hostname)
            val answers = JSONObject(body).optJSONArray("Answer") ?: JSONArray()
            val addresses = buildList {
                for (index in 0 until answers.length()) {
                    val answer = answers.getJSONObject(index)
                    if (answer.optInt("type") == 1) {
                        add(InetAddress.getByName(answer.getString("data")))
                    }
                }
            }
            if (addresses.isEmpty()) {
                throw UnknownHostException("$hostname (no A records)")
            }
            Log.i(TAG, "Resolved $hostname -> ${addresses.joinToString { it.hostAddress ?: "?" }}")
            return preferIpv4OrAll(addresses)
        }
    }

    private fun preferIpv4OrAll(addresses: List<InetAddress>): List<InetAddress> {
        val ipv4 = addresses.filter { it is Inet4Address }
        return if (ipv4.isNotEmpty()) ipv4 else addresses
    }

    private fun assertBackgroundThread(label: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw UnknownHostException("$label blocked on main thread")
        }
    }
}
