package dev.abbasian.data.mapper

import dev.abbasian.data.local.entity.CachedRiskEntity
import dev.abbasian.domain.model.RiskAssessment
import dev.abbasian.domain.model.RiskLevel

fun CachedRiskEntity.toDomain(): RiskAssessment {
    return RiskAssessment(
        packageName = this.packageName,
        riskLevel = try { RiskLevel.valueOf(this.riskLevel) } catch (e: Exception) { RiskLevel.UNKNOWN },
        threatType = this.threatType,
        description = this.description,
        timestamp = this.timestamp,
        heuristicsUsed = this.heuristicsUsed,
        ensembleMetadata = this.ensembleMetadata,
        drebinFeatures = this.drebinFeatures
    )
}

fun RiskAssessment.toEntity(
    syncStatus: dev.abbasian.data.local.entity.SyncStatus = dev.abbasian.data.local.entity.SyncStatus.SYNCED,
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
        syncStatus = syncStatus.name,
        drebinFeatures = this.drebinFeatures
    )
}
