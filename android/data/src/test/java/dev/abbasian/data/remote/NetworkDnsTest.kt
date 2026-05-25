package dev.abbasian.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address

class NetworkDnsTest {

    @Test
    fun `preferIpv4 resolves IP literals without DNS lookup`() {
        val addresses = NetworkDns.preferIpv4.lookup("8.8.8.8")

        assertEquals(1, addresses.size)
        assertEquals("8.8.8.8", addresses[0].hostAddress)
        assertTrue(addresses[0] is Inet4Address)
    }
}
