package com.codekhoda.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codekhoda.domain.model.RiskLevel

@Entity(tableName = "risk_cache")
data class CachedRiskEntity(
    @PrimaryKey val packageName: String,
    val riskLevel: String, // Stored as Enum name
    val threatType: String,
    val description: String,
    val timestamp: Long,
    val heuristicsUsed: List<String> = emptyList()
)
