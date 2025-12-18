package com.codekhoda.domain.model

data class RiskAssessment(
    val packageName: String,
    val riskLevel: RiskLevel,
    val threatType: String = "", // e.g., "Spyware", "Trojan", "Adware"
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val heuristicsUsed: List<String> = emptyList()
)
