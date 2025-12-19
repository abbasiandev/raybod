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
        heuristicsUsed = this.heuristicsUsed,
        ensembleMetadata = this.ensembleMetadata
    )
}

fun RiskAssessment.toEntity(
    syncStatus: com.codekhoda.data.local.entity.SyncStatus = com.codekhoda.data.local.entity.SyncStatus.SYNCED,
    appVersion: Long = 0,
    lastUpdateTime: Long = 0
): CachedRiskEntity {
    return CachedRiskEntity(
        packageName = this.packageName,
        riskLevel = this.riskLevel.name,
        threatType = this.threatType,
        description = this.description,
        timestamp = this.timestamp,
        appVersion = appVersion,
        lastUpdateTime = lastUpdateTime,
        heuristicsUsed = this.heuristicsUsed,
        ensembleMetadata = this.ensembleMetadata,
        syncStatus = syncStatus.name
    )
}
