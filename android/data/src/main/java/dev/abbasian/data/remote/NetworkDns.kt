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

/**
 * Android rejects hostnames with underscores via IDN.toASCII (STD3 rules).
 * JustRunMy URLs like gitr_g6pdx-727.b.jrnm.app fail on device but work from curl on Mac.
 * Underscore hosts skip system DNS and resolve via public DNS JSON APIs instead.
 */
object NetworkDns {
    private const val TAG = "RaybodCloud"

    private val systemDns: Dns = Dns.SYSTEM
    private val bootstrapClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val preferIpv4: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = resolveHostname(hostname)
            val ipv4 = addresses.filter { it is Inet4Address }
            return if (ipv4.isNotEmpty()) ipv4 else addresses
        }
    }

    private fun resolveHostname(hostname: String): List<InetAddress> {
        if (hostname.contains('_')) {
            Log.i(TAG, "Resolving underscore hostname via public DNS: $hostname")
            return resolveViaPublicDns(hostname)
        }
        return systemDns.lookup(hostname)
    }

    private fun resolveViaPublicDns(hostname: String): List<InetAddress> {
        return try {
            queryDnsJson("https://dns.google/resolve", hostname)
        } catch (googleError: Exception) {
            Log.w(TAG, "Google DNS lookup failed for $hostname: ${googleError.message}")
            try {
                queryDnsJson("https://cloudflare-dns.com/dns-query", hostname)
            } catch (cloudflareError: Exception) {
                Log.e(TAG, "Cloudflare DNS lookup failed for $hostname: ${cloudflareError.message}")
                throw UnknownHostException(hostname)
            }
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
            Log.i(TAG, "Resolved $hostname -> ${addresses.first().hostAddress}")
            return addresses
        }
    }
}
