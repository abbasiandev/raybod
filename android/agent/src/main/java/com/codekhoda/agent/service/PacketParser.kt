package com.codekhoda.agent.service

import java.nio.ByteBuffer

object PacketParser {
    
    fun extractDnsQuery(payload: ByteArray): String? {
        // Simple DNS query extraction (standard port 53)
        // This is a simplified version for demonstration
        try {
            if (payload.size < 12) return null
            // Skip DNS header (12 bytes)
            var pos = 12
            val domain = StringBuilder()
            while (pos < payload.size) {
                val len = payload[pos].toInt()
                if (len == 0) break
                pos++
                if (domain.isNotEmpty()) domain.append(".")
                domain.append(String(payload.sliceArray(pos until pos + len)))
                pos += len
            }
            return if (domain.isNotEmpty()) domain.toString() else null
        } catch (e: Exception) {
            return null
        }
    }

    fun extractSni(payload: ByteArray): String? {
        // Simple TLS SNI extraction
        // Look for TLS handshake (0x16), Client Hello (0x01)
        try {
            var pos = 0
            if (payload[pos] != 0x16.toByte()) return null
            pos += 5 // Skip TLS record header
            if (payload[pos] != 0x01.toByte()) return null
            pos += 38 // Skip Handshake header, Version, Random
            
            // Session ID
            val sessionIdLen = payload[pos].toInt()
            pos += 1 + sessionIdLen
            
            // Cipher Suites
            val cipherSuitesLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos+1].toInt() and 0xFF)
            pos += 2 + cipherSuitesLen
            
            // Compression Methods
            val compressionLen = payload[pos].toInt()
            pos += 1 + compressionLen
            
            // Extensions
            val extensionsLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos+1].toInt() and 0xFF)
            pos += 2
            val extensionsEnd = pos + extensionsLen
            
            while (pos < extensionsEnd && pos < payload.size) {
                val extType = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos+1].toInt() and 0xFF)
                val extLen = ((payload[pos+2].toInt() and 0xFF) shl 8) or (payload[pos+3].toInt() and 0xFF)
                pos += 4
                
                if (extType == 0x0000) { // Server Name Extension
                    val serverNameListLen = ((payload[pos].toInt() and 0xFF) shl 8) or (payload[pos+1].toInt() and 0xFF)
                    pos += 2
                    val serverNameType = payload[pos].toInt()
                    if (serverNameType == 0) { // host_name
                        val nameLen = ((payload[pos+1].toInt() and 0xFF) shl 8) or (payload[pos+2].toInt() and 0xFF)
                        return String(payload.sliceArray(pos + 3 until pos + 3 + nameLen))
                    }
                }
                pos += extLen
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }
}




