package com.codekhoda.domain.model

data class NetworkFlow(
    val id: String,
    val sourceApp: String, // Package name
    val destinationIp: String,
    val destinationPort: Int,
    val protocol: String, // TCP, UDP, ICMP
    val domain: String? = null,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val riskLevel: RiskLevel = RiskLevel.UNKNOWN,
    val verdict: String? = null
)
