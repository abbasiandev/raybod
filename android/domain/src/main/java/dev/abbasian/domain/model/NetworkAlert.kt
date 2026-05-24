package dev.abbasian.domain.model

data class NetworkAlert(
    val id: String,
    val packageName: String,
    val destination: String,
    val threatType: String,
    val riskLevel: RiskLevel,
    val description: String,
    val timestamp: Long
)




