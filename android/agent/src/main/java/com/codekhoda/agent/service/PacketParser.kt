package com.codekhoda.agent.service

import java.nio.ByteBuffer

object PacketParser {
    
    private const val DNS_HEADER_SIZE = 12
    
    /**
     * Extracts domain name from DNS query packet (UDP port 53).
     * 
     * Parses DNS packet format to extract the queried domain name.
     * Used for identifying which domains apps are contacting.
     * 
     * @param payload Raw DNS packet payload
     * @return Domain name or null if parsing fails
     */
    fun extractDnsQuery(payload: ByteArray): String? {
        try {
            if (payload.size < DNS_HEADER_SIZE) return null
            
            // Skip DNS header, parse domain labels
            var position = DNS_HEADER_SIZE
            val domainBuilder = StringBuilder()
            
            while (position < payload.size) {
                val labelLength = payload[position].toInt()
                if (labelLength == 0) break  // End of domain name
                
                position++
                if (domainBuilder.isNotEmpty()) domainBuilder.append(".")
                domainBuilder.append(String(payload.sliceArray(position until position + labelLength)))
                position += labelLength
            }
            
            return if (domainBuilder.isNotEmpty()) domainBuilder.toString() else null
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Extracts Server Name Indication (SNI) from TLS Client Hello packet.
     * 
     * Parses TLS handshake to extract the hostname being connected to.
     * This reveals HTTPS destinations even though content is encrypted.
     * 
     * @param payload Raw TLS packet payload
     * @return Hostname from SNI extension or null if not found
     */
    fun extractSni(payload: ByteArray): String? {
        try {
            var position = 0
            
            // Check for TLS handshake record (0x16)
            if (payload[position] != 0x16.toByte()) return null
            position += 5  // Skip TLS record header
            
            // Check for Client Hello message (0x01)
            if (payload[position] != 0x01.toByte()) return null
            position += 38  // Skip handshake header, version, random
            
            // Parse Session ID length and skip
            val sessionIdLength = payload[position].toInt()
            position += 1 + sessionIdLength
            
            // Parse and skip Cipher Suites
            val cipherSuitesLength = ((payload[position].toInt() and 0xFF) shl 8) or (payload[position+1].toInt() and 0xFF)
            position += 2 + cipherSuitesLength
            
            // Parse and skip Compression Methods
            val compressionLength = payload[position].toInt()
            position += 1 + compressionLength
            
            // Parse Extensions (where SNI lives)
            val extensionsLength = ((payload[position].toInt() and 0xFF) shl 8) or (payload[position+1].toInt() and 0xFF)
            position += 2
            val extensionsEnd = position + extensionsLength
            
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




