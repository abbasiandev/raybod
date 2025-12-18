package com.codekhoda.data.mapper

import com.codekhoda.data.local.entity.CachedRiskEntity
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel

fun CachedRiskEntity.toDomain(): RiskAssessment {
    return RiskAssessment(
        packageName = this.packageName,
        riskLevel = try { RiskLevel.valueOf(this.riskLevel) } catch (e: Exception) { RiskLevel.UNKNOWN },
        threatType = this.threatType,
        description = this.description,
        timestamp = this.timestamp,
        heuristicsUsed = this.heuristicsUsed
    )
}

fun RiskAssessment.toEntity(syncStatus: com.codekhoda.data.local.entity.SyncStatus = com.codekhoda.data.local.entity.SyncStatus.SYNCED): CachedRiskEntity {
    return CachedRiskEntity(
        packageName = this.packageName,
        riskLevel = this.riskLevel.name,
        threatType = this.threatType,
        description = this.description,
        timestamp = this.timestamp,
        heuristicsUsed = this.heuristicsUsed,
        syncStatus = syncStatus.name
    )
}
