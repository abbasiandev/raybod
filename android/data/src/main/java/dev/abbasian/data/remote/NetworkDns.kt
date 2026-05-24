package dev.abbasian.data.remote

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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val preferIpv4: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            if (IP_LITERAL.matches(hostname)) {
                return listOf(InetAddress.getByName(hostname))
            }
            if (hostname.contains('_')) {
                return lookupPublicIpv4(hostname)
            }
            val addresses = systemDns.lookup(hostname)
            val ipv4 = addresses.filter { it is Inet4Address }
            return if (ipv4.isNotEmpty()) ipv4 else addresses
        }
    }

    fun lookupPublicIpv4(hostname: String): List<InetAddress> {
        Log.i(TAG, "Public DNS lookup for: $hostname")
        return try {
            queryDnsJson("https://dns.google/resolve", hostname)
        } catch (googleError: Exception) {
            Log.w(TAG, "Google DNS failed for $hostname: ${googleError.message}")
            queryDnsJson("https://cloudflare-dns.com/dns-query", hostname)
        }
    }

    private fun queryDnsJson(baseUrl: String, hostname: String): List<InetAddress> {
        val encodedHost = URLEncoder.encode(hostname, Charsets.UTF_8.name())
        val separator = if (baseUrl.contains("?")) "&" else "?"
        val request = Request.Builder()
            .url("${baseUrl}${separator}name=$encodedHost&type=A")
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
            return addresses.filterIsInstance<Inet4Address>().ifEmpty { addresses }
        }
    }
}
