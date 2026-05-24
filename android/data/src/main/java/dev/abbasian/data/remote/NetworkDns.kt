package dev.abbasian.data.remote

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

/**
 * Some mobile networks fail IPv6 routes with opaque "Android internal error" from OkHttp.
 * Prefer IPv4 addresses when both are available.
 */
object NetworkDns {
    private val systemDns: Dns = Dns.SYSTEM

    val preferIpv4: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = systemDns.lookup(hostname)
            val ipv4 = addresses.filter { it is Inet4Address }
            return if (ipv4.isNotEmpty()) ipv4 else addresses
        }
    }
}
