package dev.abbasian.data.repository

import dev.abbasian.data.local.dao.RiskDao
import dev.abbasian.data.mapper.toDomain
import dev.abbasian.data.mapper.toEntity
import dev.abbasian.domain.model.AppPackage
import dev.abbasian.domain.model.RiskAssessment
import dev.abbasian.domain.model.RiskLevel
import dev.abbasian.domain.repository.ThreatRepository
import javax.inject.Inject

class ThreatRepositoryImpl @Inject constructor(
    private val riskDao: RiskDao,
    private val api: dev.abbasian.data.remote.api.CloudBrainApi,
    private val malwareScanner: dev.abbasian.data.ml.MalwareScanner
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
                        syncStatus = dev.abbasian.data.local.entity.SyncStatus.SYNCED,
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
        if (dev.abbasian.domain.filter.SystemPackageFilter.isTrustedSignature(appPackage.signature)) {
            val trustedResult = RiskAssessment(
                packageName = appPackage.packageName,
                riskLevel = RiskLevel.SAFE,
                description = "Trusted Developer Signature Detected",
                heuristicsUsed = listOf("Reputation: Trusted Developer")
            )
            riskDao.insertRisk(trustedResult.toEntity(
                syncStatus = dev.abbasian.data.local.entity.SyncStatus.SYNCED,
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
                 syncStatus = dev.abbasian.data.local.entity.SyncStatus.LOCAL_ONLY,
                 appVersion = appPackage.versionCode,
                 lastUpdateTime = appPackage.lastUpdateTime
             ))
             return localResult
        }

        // 3. Call Cloud API for Second Opinion (if not critical)
        var syncStatus = dev.abbasian.data.local.entity.SyncStatus.SYNCED
        val finalResult = try {
            val dto = dev.abbasian.data.remote.dto.AppMetadataDto(
                packageName = appPackage.packageName,
                versionCode = appPackage.versionCode,
                signature = appPackage.signature,
                permissions = appPackage.permissions,
                ensembleMetadata = localResult.ensembleMetadata,
                // NEW: Send additional metadata
                intents = appPackage.intents,
                versionName = appPackage.versionName,
                installTime = appPackage.installTime,
                lastUpdateTime = appPackage.lastUpdateTime,
                hasReflection = appPackage.hasReflection,
                hasDynamicLoading = appPackage.hasDynamicLoading
            )
            val response = api.analyzeApp(dto)
            
            val drebin = response.drebinFeatures?.let {
                dev.abbasian.domain.model.DrebinFeatures(
                    s1Hardware = it.s1Hardware,
                    s2RequestedPermissions = it.s2RequestedPermissions,
                    s3AppComponents = it.s3AppComponents,
                    s4FilteredIntents = it.s4FilteredIntents,
                    s5RestrictedApis = it.s5RestrictedApis,
                    s6UsedPermissions = it.s6UsedPermissions,
                    s7SuspiciousApis = it.s7SuspiciousApis,
                    s8NetworkAddresses = it.s8NetworkAddresses
                )
            } ?: dev.abbasian.domain.model.DrebinFeatures()

            RiskAssessment(
                packageName = response.packageName,
                riskLevel = try { RiskLevel.valueOf(response.riskLevel) } catch (e: Exception) { RiskLevel.UNKNOWN },
                threatType = response.threatType,
                description = response.description,
                heuristicsUsed = response.heuristicsUsed,
                drebinFeatures = drebin
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to local AI result if cloud fails and mark as PENDING
            syncStatus = dev.abbasian.data.local.entity.SyncStatus.PENDING
            
            // Trigger WorkManager for background retry
            try {
                val context = (api as? dev.abbasian.data.remote.api.CloudBrainApi)?.let { null } // Hacky way to say we need context
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
                    dev.abbasian.data.remote.dto.AppMetadataDto(
                        packageName = pkg.packageName,
                        versionCode = pkg.versionCode,
                        signature = pkg.signature,
                        permissions = pkg.permissions,
                        intents = pkg.intents,
                        versionName = pkg.versionName,
                        installTime = pkg.installTime,
                        lastUpdateTime = pkg.lastUpdateTime,
                        hasReflection = pkg.hasReflection,
                        hasDynamicLoading = pkg.hasDynamicLoading
                    )
                }
                val response = api.batchScan(dev.abbasian.data.remote.dto.BatchScanRequestDto(dtos))
                
                return response.results.map { res ->
                    val drebin = res.drebinFeatures?.let {
                        dev.abbasian.domain.model.DrebinFeatures(
                            s1Hardware = it.s1Hardware,
                            s2RequestedPermissions = it.s2RequestedPermissions,
                            s3AppComponents = it.s3AppComponents,
                            s4FilteredIntents = it.s4FilteredIntents,
                            s5RestrictedApis = it.s5RestrictedApis,
                            s6UsedPermissions = it.s6UsedPermissions,
                            s7SuspiciousApis = it.s7SuspiciousApis,
                            s8NetworkAddresses = it.s8NetworkAddresses
                        )
                    } ?: dev.abbasian.domain.model.DrebinFeatures()

                    val assessment = RiskAssessment(
                        packageName = res.packageName,
                        riskLevel = try { RiskLevel.valueOf(res.riskLevel) } catch (e: Exception) { RiskLevel.UNKNOWN },
                        threatType = res.threatType,
                        description = res.description,
                        heuristicsUsed = res.heuristicsUsed,
                        drebinFeatures = drebin
                    )
                    // Cache results
                    riskDao.insertRisk(assessment.toEntity(
                        syncStatus = dev.abbasian.data.local.entity.SyncStatus.SYNCED,
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
