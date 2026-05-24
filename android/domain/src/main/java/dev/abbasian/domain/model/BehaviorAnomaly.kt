package dev.abbasian.domain.model

enum class AnomalyType {
    UNEXPECTED_BACKGROUND_ACCESS,
    CONTEXT_MISMATCH,
    SILENT_DATA_EXFILTRATION,
    HIGH_FREQUENCY_ACCESS
}

data class BehaviorAnomaly(
    val packageName: String,
    val anomalyType: AnomalyType,
    val description: String,
    val severity: Float, // 0.0 to 1.0
    val timestamp: Long = System.currentTimeMillis()
)
