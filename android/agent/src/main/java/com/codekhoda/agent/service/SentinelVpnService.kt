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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        Log.d("SentinelVpn", "Starting VPN Service...")
        networkRepository.clearActiveFlows()
        
        val builder = Builder()
            .setSession("SentinelVpn")
            .addAddress("10.0.0.1", 24)
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)
        
        // In a real implementation, we would exclude our own package to avoid loops
        builder.addDisallowedApplication(packageName)

        vpnInterface = builder.establish()
        
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
        val packet = ByteBuffer.allocate(Short.MAX_VALUE.toInt())

        val flows = mutableMapOf<String, NetworkFlow>()

        while (isRunning && currentCoroutineContext().isActive) {
            val length = withContext(Dispatchers.IO) {
                inputStream.read(packet.array())
            }

            if (length > 0) {
                // Parse IP Header (basic IPv4)
                val buffer = packet.array()
                if ((buffer[0].toInt() shr 4) == 4) { // IPv4
                    val protocol = buffer[9].toInt()
                    val srcIp = InetAddress.getByAddress(buffer.sliceArray(12..15)).hostAddress
                    val dstIp = InetAddress.getByAddress(buffer.sliceArray(16..19)).hostAddress
                    
                    var destinationPort = 0
                    var domain: String? = null
                    
                    // Parse TCP/UDP for ports and domain extraction
                    val headerOffset = (buffer[0].toInt() and 0x0F) * 4
                    val payloadOffset = when (protocol) {
                        6 -> { // TCP
                            destinationPort = ((buffer[headerOffset + 2].toInt() and 0xFF) shl 8) or (buffer[headerOffset + 3].toInt() and 0xFF)
                            headerOffset + ((buffer[headerOffset + 12].toInt() shr 4) and 0x0F) * 4
                        }
                        17 -> { // UDP
                            destinationPort = ((buffer[headerOffset + 2].toInt() and 0xFF) shl 8) or (buffer[headerOffset + 3].toInt() and 0xFF)
                            headerOffset + 8
                        }
                        else -> headerOffset
                    }

                    val payload = if (length > payloadOffset) buffer.sliceArray(payloadOffset until length) else null
                    
                    if (payload != null) {
                        if (protocol == 17 && destinationPort == 53) {
                            domain = PacketParser.extractDnsQuery(payload)
                        } else if (protocol == 6 && (destinationPort == 443 || destinationPort == 8443)) {
                            domain = PacketParser.extractSni(payload)
                        }
                    }

                    // Simple flow identification
                    val flowKey = "$dstIp-$destinationPort-$protocol"
                    val flow = flows.getOrPut(flowKey) {
                        NetworkFlow(
                            id = java.util.UUID.randomUUID().toString(),
                            sourceApp = "unknown", 
                            destinationIp = dstIp,
                            destinationPort = destinationPort,
                            protocol = when(protocol) {
                                6 -> "TCP"
                                17 -> "UDP"
                                1 -> "ICMP"
                                else -> "Other"
                            },
                            domain = domain
                        )
                    }

                    // Update flow stats
                    val updatedFlow = flow.copy(
                        bytesSent = flow.bytesSent + length,
                        timestamp = System.currentTimeMillis()
                    )
                    flows[flowKey] = updatedFlow

                    // Periodic sync with Repository - more frequent for responsiveness
                    val lastSync = flows.values.maxOfOrNull { it.timestamp } ?: 0
                    if (flows.size >= 5 || System.currentTimeMillis() - lastSync > 2000) {
                        networkRepository.analyzeFlows(flows.values.toList().sortedByDescending { it.timestamp })
                    }
                }

                // Write packet back to system (forwarding)
                // In a real VPN, we would forward to the real network
                withContext(Dispatchers.IO) {
                    outputStream.write(packet.array(), 0, length)
                }
                packet.clear()
            }
        }
    }

    override fun onDestroy() {
        isRunning = false
        networkRepository.clearActiveFlows()
        serviceScope.cancel()
        vpnInterface?.close()
        super.onDestroy()
        Log.d("SentinelVpn", "VPN Service stopped")
    }
}
