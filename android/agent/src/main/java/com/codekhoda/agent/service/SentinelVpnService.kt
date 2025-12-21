package com.codekhoda.agent.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.codekhoda.domain.model.NetworkFlow
import com.codekhoda.domain.repository.NetworkRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
class SentinelVpnService : VpnService() {

    @Inject
    lateinit var networkRepository: NetworkRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    private companion object {
        const val TAG = "SentinelVpn"
        const val VPN_SESSION_NAME = "SentinelVpn"
        const val VPN_ADDRESS = "10.0.0.1"
        const val VPN_PREFIX_LENGTH = 24
        const val DNS_SERVER = "8.8.8.8"
        const val ROUTE_ADDRESS = "0.0.0.0"
        const val ROUTE_PREFIX_LENGTH = 0
        const val PACKET_BUFFER_SIZE = 32767  // Short.MAX_VALUE
        const val FLOW_SYNC_COUNT_THRESHOLD = 5
        const val FLOW_SYNC_TIME_THRESHOLD_MS = 2000L
        const val IP_VERSION_4 = 4
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17
        const val PROTOCOL_ICMP = 1
        const val PORT_DNS = 53
        const val PORT_HTTPS = 443
        const val PORT_HTTPS_ALT = 8443
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        Log.d(TAG, "Starting VPN Service...")
        networkRepository.clearActiveFlows()
        
        val vpnBuilder = Builder()
            .setSession(VPN_SESSION_NAME)
            .addAddress(VPN_ADDRESS, VPN_PREFIX_LENGTH)
            .addDnsServer(DNS_SERVER)
            .addRoute(ROUTE_ADDRESS, ROUTE_PREFIX_LENGTH)
        
        // Exclude our own package to prevent VPN loops
        vpnBuilder.addDisallowedApplication(packageName)

        vpnInterface = vpnBuilder.establish()
        
        if (vpnInterface != null) {
            isRunning = true
            serviceScope.launch {
                runVpnLoop()
            }
        }
    }

    private suspend fun runVpnLoop() {
        val inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
        val outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)
        val packetBuffer = ByteBuffer.allocate(PACKET_BUFFER_SIZE)

        val activeFlows = mutableMapOf<String, NetworkFlow>()

        while (isRunning && currentCoroutineContext().isActive) {
            val bytesRead = withContext(Dispatchers.IO) {
                inputStream.read(packetBuffer.array())
            }

            if (bytesRead > 0) {
                // Parse IP header (IPv4 only)
                val packetData = packetBuffer.array()
                val ipVersion = (packetData[0].toInt() shr 4)
                
                if (ipVersion == IP_VERSION_4) {
                    val protocol = packetData[9].toInt()
                    val sourceIp = InetAddress.getByAddress(packetData.sliceArray(12..15)).hostAddress
                    val destinationIp = InetAddress.getByAddress(packetData.sliceArray(16..19)).hostAddress
                    
                    var destinationPort = 0
                    var extractedDomain: String? = null
                    
                    // Parse transport layer headers (TCP/UDP) for port and domain extraction
                    val ipHeaderLength = (packetData[0].toInt() and 0x0F) * 4
                    val payloadOffset = when (protocol) {
                        PROTOCOL_TCP -> {
                            destinationPort = ((packetData[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or 
                                             (packetData[ipHeaderLength + 3].toInt() and 0xFF)
                            val tcpHeaderLength = ((packetData[ipHeaderLength + 12].toInt() shr 4) and 0x0F) * 4
                            ipHeaderLength + tcpHeaderLength
                        }
                        PROTOCOL_UDP -> {
                            destinationPort = ((packetData[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or 
                                             (packetData[ipHeaderLength + 3].toInt() and 0xFF)
                            ipHeaderLength + 8
                        }
                        else -> ipHeaderLength
                    }

                    val payload = if (bytesRead > payloadOffset) {
                        packetData.sliceArray(payloadOffset until bytesRead)
                    } else null
                    
                    // Extract domain from DNS queries or TLS SNI
                    if (payload != null) {
                        extractedDomain = when {
                            protocol == PROTOCOL_UDP && destinationPort == PORT_DNS -> 
                                PacketParser.extractDnsQuery(payload)
                            protocol == PROTOCOL_TCP && (destinationPort == PORT_HTTPS || destinationPort == PORT_HTTPS_ALT) -> 
                                PacketParser.extractSni(payload)
                            else -> null
                        }
                    }

                    // Create unique flow identifier
                    val flowKey = "$destinationIp-$destinationPort-$protocol"
                    val currentFlow = activeFlows.getOrPut(flowKey) {
                        NetworkFlow(
                            id = java.util.UUID.randomUUID().toString(),
                            sourceApp = "unknown", 
                            destinationIp = destinationIp,
                            destinationPort = destinationPort,
                            protocol = when(protocol) {
                                PROTOCOL_TCP -> "TCP"
                                PROTOCOL_UDP -> "UDP"
                                PROTOCOL_ICMP -> "ICMP"
                                else -> "Other"
                            },
                            domain = extractedDomain
                        )
                    }

                    // Update flow statistics
                    val updatedFlow = currentFlow.copy(
                        bytesSent = currentFlow.bytesSent + bytesRead,
                        timestamp = System.currentTimeMillis()
                    )
                    activeFlows[flowKey] = updatedFlow

                    // Periodically sync flows with repository for analysis
                    val lastSyncTime = activeFlows.values.maxOfOrNull { it.timestamp } ?: 0
                    val shouldSync = activeFlows.size >= FLOW_SYNC_COUNT_THRESHOLD || 
                                    System.currentTimeMillis() - lastSyncTime > FLOW_SYNC_TIME_THRESHOLD_MS
                    
                    if (shouldSync) {
                        val sortedFlows = activeFlows.values.toList().sortedByDescending { it.timestamp }
                        networkRepository.analyzeFlows(sortedFlows)
                    }
                }

                // Forward packet back to system
                // In production, this would route through actual network interfaces
                withContext(Dispatchers.IO) {
                    outputStream.write(packetBuffer.array(), 0, bytesRead)
                }
                packetBuffer.clear()
            }
        }
    }

    override fun onDestroy() {
        isRunning = false
        networkRepository.clearActiveFlows()
        serviceScope.cancel()
        vpnInterface?.close()
        super.onDestroy()
        Log.d(TAG, "VPN Service stopped")
    }
}
