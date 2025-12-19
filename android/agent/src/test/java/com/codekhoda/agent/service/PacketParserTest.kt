package com.codekhoda.agent.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PacketParserTest {

    @Test
    fun `extractDnsQuery - correctly extracts google_com`() {
        // DNS Query for google.com (simplified)
        // 12 bytes header + (6)google(3)com(0)
        val payload = byteArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // Header
            6, 'g'.toByte(), 'o'.toByte(), 'o'.toByte(), 'g'.toByte(), 'l'.toByte(), 'e'.toByte(),
            3, 'c'.toByte(), 'o'.toByte(), 'm'.toByte(),
            0
        )
        
        val domain = PacketParser.extractDnsQuery(payload)
        assertEquals("google.com", domain)
    }

    @Test
    fun `extractDnsQuery - returns null for empty payload`() {
        val payload = byteArrayOf()
        assertNull(PacketParser.extractDnsQuery(payload))
    }

    @Test
    fun `extractSni - correctly extracts SNI from TLS Client Hello`() {
        // Simplified TLS Client Hello with SNI google.com
        val payload = ByteArray(200)
        payload[0] = 0x16.toByte() // Handshake
        payload[5] = 0x01.toByte() // Client Hello
        
        // Skip some bytes to reach extensions (this is a very simplified mock)
        // The parser logic is:
        // pos 0: 0x16
        // pos 5: 0x01
        // pos 5 + 38 = 43 (Skip Handshake header, Version, Random)
        // pos 43: Session ID Len (0)
        // pos 44: Cipher Suites Len (2) -> pos 46
        // pos 46: Compression Methods Len (1) -> pos 48
        // pos 48: Extensions Len (20) -> pos 50
        
        payload[43] = 0 // Session ID Len
        payload[44] = 0; payload[45] = 2 // Cipher Suites Len
        payload[48] = 0 // Compression Len
        payload[49] = 0; payload[50] = 20 // Extensions Len (2 bytes)
        
        var pos = 51
        payload[pos++] = 0x00; payload[pos++] = 0x00 // Extension Type: SNI
        payload[pos++] = 0x00; payload[pos++] = 13 // Extension Len
        
        payload[pos++] = 0x00; payload[pos++] = 11 // Server Name List Len
        payload[pos++] = 0 // Server Name Type: host_name
        payload[pos++] = 0; payload[pos++] = 10 // Name Len: google.com is 10 chars
        
        val name = "google.com"
        name.forEachIndexed { i, c -> payload[pos + i] = c.code.toByte() }
        
        val sni = PacketParser.extractSni(payload)
        assertEquals("google.com", sni)
    }

    @Test
    fun `extractSni - returns null for non-TLS payload`() {
        val payload = byteArrayOf(0x00, 0x01, 0x02)
        assertNull(PacketParser.extractSni(payload))
    }
}

