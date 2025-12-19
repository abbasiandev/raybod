package com.codekhoda.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codekhoda.domain.model.DrebinFeatures
import com.codekhoda.domain.model.RiskLevel

@Entity(tableName = "risk_cache")
data class CachedRiskEntity(
    @PrimaryKey val packageName: String,
    val riskLevel: String, // Stored as Enum name
    val threatType: String,
    val description: String,
    val timestamp: Long,
    val appVersion: Long = 0,
    val lastUpdateTime: Long = 0,
    val heuristicsUsed: List<String> = emptyList(),
    val ensembleMetadata: Map<String, Float> = emptyMap(),
    val syncStatus: String = "SYNCED", // SYNCED, PENDING, LOCAL_ONLY
    val drebinFeatures: DrebinFeatures = DrebinFeatures()
)

enum class SyncStatus {
    SYNCED, PENDING, LOCAL_ONLY
}
