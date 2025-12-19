package com.codekhoda.data.repository

import com.codekhoda.data.local.dao.RiskDao
import com.codekhoda.data.mapper.toDomain
import com.codekhoda.data.mapper.toEntity
import com.codekhoda.domain.model.AppPackage
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.domain.repository.ThreatRepository
import javax.inject.Inject

class ThreatRepositoryImpl @Inject constructor(
    private val riskDao: RiskDao,
    private val api: com.codekhoda.data.remote.api.CloudBrainApi,
    private val malwareScanner: com.codekhoda.data.ml.MalwareScanner
) : ThreatRepository {

    override suspend fun scanApp(appPackage: AppPackage, lowSpeedMode: Boolean): RiskAssessment {

        // 0. Fast Cloud Allowlist Check (Category 5.1 Optimization)
        if (!lowSpeedMode) {
            try {
                val allowlistCheck = api.checkAllowlist(appPackage.packageName)
                if (allowlistCheck.isAllowed) {
                    val safeResult = RiskAssessment(
                        packageName = appPackage.packageName,
                        riskLevel = RiskLevel.SAFE,
                        description = "Verified Safe via Global Cloud Allowlist",
                        heuristicsUsed = listOf("Cloud: Global Allowlist")
                    )
                    riskDao.insertRisk(safeResult.toEntity(
                        syncStatus = com.codekhoda.data.local.entity.SyncStatus.SYNCED,
                        appVersion = appPackage.versionCode,
                        lastUpdateTime = appPackage.lastUpdateTime
                    ))
                    return safeResult
                }
            } catch (e: Exception) {
                // Fail silent and continue to local scan
            }
        }

        // 1. Check local cache & Differential Scanning (Category 5.2)
        val cached = riskDao.getRisk(appPackage.packageName)
        if (cached != null && cached.lastUpdateTime == appPackage.lastUpdateTime) {
            return cached.toDomain()
        }
        
        // 1.1 Reputation Caching: Skip if trusted signature (Category 5.3)
        if (com.codekhoda.domain.filter.SystemPackageFilter.isTrustedSignature(appPackage.signature)) {
            val trustedResult = RiskAssessment(
                packageName = appPackage.packageName,
                riskLevel = RiskLevel.SAFE,
                description = "Trusted Developer Signature Detected",
                heuristicsUsed = listOf("Reputation: Trusted Developer")
            )
            riskDao.insertRisk(trustedResult.toEntity(
                syncStatus = com.codekhoda.data.local.entity.SyncStatus.SYNCED,
                appVersion = appPackage.versionCode,
                lastUpdateTime = appPackage.lastUpdateTime
            ))
            return trustedResult
        }

        // 2. Perform On-Device AI Scan (First Line of Defense)
        val localResult = malwareScanner.scan(appPackage)
        
        // If critical, return immediately (Blocking threat)
        if (localResult.riskLevel == RiskLevel.CRITICAL) {
             riskDao.insertRisk(localResult.toEntity(
                 syncStatus = com.codekhoda.data.local.entity.SyncStatus.LOCAL_ONLY,
                 appVersion = appPackage.versionCode,
                 lastUpdateTime = appPackage.lastUpdateTime
             ))
             return localResult
        }

        // 3. Call Cloud API for Second Opinion (if not critical)
        var syncStatus = com.codekhoda.data.local.entity.SyncStatus.SYNCED
        val finalResult = try {
            val dto = com.codekhoda.data.remote.dto.AppMetadataDto(
                packageName = appPackage.packageName,
                versionCode = appPackage.versionCode,
                signature = appPackage.signature,
                permissions = appPackage.permissions,
                ensembleMetadata = localResult.ensembleMetadata,
                // NEW: Send additional metadata
                intents = appPackage.intents,
                versionName = appPackage.versionName,
                installTime = appPackage.installTime,
                lastUpdateTime = appPackage.lastUpdateTime
            )
            val response = api.analyzeApp(dto)
            
            RiskAssessment(
                packageName = response.packageName,
                riskLevel = try { RiskLevel.valueOf(response.riskLevel) } catch (e: Exception) { RiskLevel.UNKNOWN },
                threatType = response.threatType,
                description = response.description,
                heuristicsUsed = response.heuristicsUsed
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to local AI result if cloud fails and mark as PENDING
            syncStatus = com.codekhoda.data.local.entity.SyncStatus.PENDING
            
            // Trigger WorkManager for background retry
            try {
                val context = (api as? com.codekhoda.data.remote.api.CloudBrainApi)?.let { null } // Hacky way to say we need context
                // In a real app, we'd inject WorkManager or a SyncScheduler
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            
            localResult
        }

        // 4. Cache result
        riskDao.insertRisk(finalResult.toEntity(
            syncStatus = syncStatus,
            appVersion = appPackage.versionCode,
            lastUpdateTime = appPackage.lastUpdateTime
        ))

        return finalResult
    }

    override suspend fun scanApps(appPackages: List<AppPackage>): List<RiskAssessment> {
        // Optimization: For large sets, use batch API (Category 5.1)
        if (appPackages.size > 5) {
            try {
                val dtos = appPackages.map { pkg ->
                    com.codekhoda.data.remote.dto.AppMetadataDto(
                        packageName = pkg.packageName,
                        versionCode = pkg.versionCode,
                        signature = pkg.signature,
                        permissions = pkg.permissions,
                        intents = pkg.intents,
                        versionName = pkg.versionName,
                        installTime = pkg.installTime,
                        lastUpdateTime = pkg.lastUpdateTime
                    )
                }
                val response = api.batchScan(com.codekhoda.data.remote.dto.BatchScanRequestDto(dtos))
                
                return response.results.map { res ->
                    val assessment = RiskAssessment(
                        packageName = res.packageName,
                        riskLevel = try { RiskLevel.valueOf(res.riskLevel) } catch (e: Exception) { RiskLevel.UNKNOWN },
                        threatType = res.threatType,
                        description = res.description,
                        heuristicsUsed = res.heuristicsUsed
                    )
                    // Cache results
                    riskDao.insertRisk(assessment.toEntity(
                        syncStatus = com.codekhoda.data.local.entity.SyncStatus.SYNCED,
                        appVersion = 0, // In batch we might not have versions easily available for all
                        lastUpdateTime = 0
                    ))
                    assessment
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to sequential scanning if batch fails
            }
        }
        return appPackages.map { scanApp(it) }
    }
}
