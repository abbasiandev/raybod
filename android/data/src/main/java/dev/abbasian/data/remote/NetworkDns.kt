package dev.abbasian.data.remote

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
 * Falls back to Google DNS-over-HTTPS JSON API when system DNS throws.
 */
object NetworkDns {
    private val systemDns: Dns = Dns.SYSTEM
    private val bootstrapClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val preferIpv4: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = resolveHostname(hostname)
            val ipv4 = addresses.filter { it is Inet4Address }
            return if (ipv4.isNotEmpty()) ipv4 else addresses
        }
    }

    private fun resolveHostname(hostname: String): List<InetAddress> {
        return try {
            systemDns.lookup(hostname)
        } catch (systemError: Exception) {
            if (hostname.contains('_')) {
                resolveViaGoogleDns(hostname)
            } else {
                throw systemError
            }
        }
    }

    private fun resolveViaGoogleDns(hostname: String): List<InetAddress> {
        val encodedHost = URLEncoder.encode(hostname, Charsets.UTF_8.name())
        val request = Request.Builder()
            .url("https://dns.google/resolve?name=$encodedHost&type=A")
            .header("Accept", "application/dns-json")
            .build()

        bootstrapClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw UnknownHostException(hostname)
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
                throw UnknownHostException(hostname)
            }
            return addresses
        }
    }
}
