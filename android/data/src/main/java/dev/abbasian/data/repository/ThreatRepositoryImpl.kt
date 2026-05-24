package dev.abbasian.data.repository

import dev.abbasian.data.local.dao.RiskDao
import dev.abbasian.data.mapper.toDomain
import dev.abbasian.data.mapper.toEntity
import dev.abbasian.data.remote.dto.AppMetadataDto
import dev.abbasian.data.remote.dto.ScanResultDto
import dev.abbasian.domain.model.AppPackage
import dev.abbasian.domain.model.DrebinFeatures
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
                    reportToCloud(appPackage, safeResult)
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
            val cachedResult = cached.toDomain()
            reportToCloud(appPackage, cachedResult)
            return cachedResult
        }

        // 1.1 Reputation Caching: Skip on-device ML if trusted signature (Category 5.3)
        if (dev.abbasian.domain.filter.SystemPackageFilter.isTrustedSignature(appPackage.signature)) {
            val trustedResult = RiskAssessment(
                packageName = appPackage.packageName,
                riskLevel = RiskLevel.SAFE,
                description = "Trusted Developer Signature Detected",
                heuristicsUsed = listOf("Reputation: Trusted Developer")
            )
            reportToCloud(appPackage, trustedResult)
            riskDao.insertRisk(trustedResult.toEntity(
                syncStatus = dev.abbasian.data.local.entity.SyncStatus.SYNCED,
                appVersion = appPackage.versionCode,
                lastUpdateTime = appPackage.lastUpdateTime
            ))
            return trustedResult
        }

        // 2. Perform On-Device AI Scan (First Line of Defense)
        val localResult = malwareScanner.scan(appPackage)

        // 3. Always report to cloud so admin analytics panel receives scan logs
        val cloudResult = reportToCloud(appPackage, localResult)
        val finalResult = mergeAssessments(localResult, cloudResult)
        val syncStatus = if (cloudResult != null) {
            dev.abbasian.data.local.entity.SyncStatus.SYNCED
        } else {
            dev.abbasian.data.local.entity.SyncStatus.PENDING
        }

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
                val dtos = appPackages.map { buildMetadataDto(it) }
                val response = api.batchScan(dev.abbasian.data.remote.dto.BatchScanRequestDto(dtos))

                return response.results.map { res ->
                    val assessment = res.toRiskAssessment()
                    riskDao.insertRisk(assessment.toEntity(
                        syncStatus = dev.abbasian.data.local.entity.SyncStatus.SYNCED,
                        appVersion = 0,
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

    /**
     * POST /api/v1/scan/analyze — creates ScanLog entries visible in the admin panel.
     * Returns cloud assessment when available; failures are logged but do not block the scan.
     */
    private suspend fun reportToCloud(
        appPackage: AppPackage,
        localResult: RiskAssessment?
    ): RiskAssessment? {
        return try {
            api.analyzeApp(buildMetadataDto(appPackage, localResult)).toRiskAssessment()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildMetadataDto(
        appPackage: AppPackage,
        localResult: RiskAssessment? = null
    ): AppMetadataDto {
        return AppMetadataDto(
            packageName = appPackage.packageName,
            versionCode = appPackage.versionCode,
            signature = appPackage.signature,
            permissions = appPackage.permissions,
            ensembleMetadata = localResult?.ensembleMetadata,
            intents = appPackage.intents,
            versionName = appPackage.versionName,
            installTime = appPackage.installTime,
            lastUpdateTime = appPackage.lastUpdateTime,
            hasReflection = appPackage.hasReflection,
            hasDynamicLoading = appPackage.hasDynamicLoading
        )
    }

    private fun ScanResultDto.toRiskAssessment(): RiskAssessment {
        val drebin = drebinFeatures?.let {
            DrebinFeatures(
                s1Hardware = it.s1Hardware,
                s2RequestedPermissions = it.s2RequestedPermissions,
                s3AppComponents = it.s3AppComponents,
                s4FilteredIntents = it.s4FilteredIntents,
                s5RestrictedApis = it.s5RestrictedApis,
                s6UsedPermissions = it.s6UsedPermissions,
                s7SuspiciousApis = it.s7SuspiciousApis,
                s8NetworkAddresses = it.s8NetworkAddresses
            )
        } ?: DrebinFeatures()

        return RiskAssessment(
            packageName = packageName,
            riskLevel = try {
                RiskLevel.valueOf(riskLevel)
            } catch (e: Exception) {
                RiskLevel.UNKNOWN
            },
            threatType = threatType,
            description = description,
            heuristicsUsed = heuristicsUsed,
            drebinFeatures = drebin
        )
    }

    private fun mergeAssessments(
        local: RiskAssessment,
        cloud: RiskAssessment?
    ): RiskAssessment {
        if (cloud == null) return local
        return if (riskSeverity(local.riskLevel) >= riskSeverity(cloud.riskLevel)) local else cloud
    }

    private fun riskSeverity(level: RiskLevel): Int = when (level) {
        RiskLevel.CRITICAL -> 6
        RiskLevel.HIGH -> 5
        RiskLevel.MEDIUM -> 4
        RiskLevel.LOW -> 3
        RiskLevel.UNKNOWN -> 2
        RiskLevel.SAFE -> 1
    }
}
