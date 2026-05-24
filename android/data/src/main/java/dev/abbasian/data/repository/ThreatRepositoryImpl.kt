package dev.abbasian.data.repository

import android.util.Log
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

    override suspend fun scanApp(
        appPackage: AppPackage,
        lowSpeedMode: Boolean,
        syncToCloud: Boolean
    ): RiskAssessment {

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
                    if (syncToCloud) {
                        reportToCloud(appPackage, safeResult)
                    }
                    riskDao.insertRisk(safeResult.toEntity(
                        syncStatus = dev.abbasian.data.local.entity.SyncStatus.SYNCED,
                        appVersion = appPackage.versionCode,
                        lastUpdateTime = appPackage.lastUpdateTime
                    ))
                    return safeResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Allowlist check failed for ${appPackage.packageName}", e)
            }
        }

        val cached = riskDao.getRisk(appPackage.packageName)
        if (cached != null && cached.lastUpdateTime == appPackage.lastUpdateTime) {
            val cachedResult = cached.toDomain()
            if (syncToCloud) {
                reportToCloud(appPackage, cachedResult)
            }
            return cachedResult
        }

        if (dev.abbasian.domain.filter.SystemPackageFilter.isTrustedSignature(appPackage.signature)) {
            val trustedResult = RiskAssessment(
                packageName = appPackage.packageName,
                riskLevel = RiskLevel.SAFE,
                description = "Trusted Developer Signature Detected",
                heuristicsUsed = listOf("Reputation: Trusted Developer")
            )
            if (syncToCloud) {
                reportToCloud(appPackage, trustedResult)
            }
            riskDao.insertRisk(trustedResult.toEntity(
                syncStatus = dev.abbasian.data.local.entity.SyncStatus.SYNCED,
                appVersion = appPackage.versionCode,
                lastUpdateTime = appPackage.lastUpdateTime
            ))
            return trustedResult
        }

        val localResult = malwareScanner.scan(appPackage)

        if (!syncToCloud) {
            riskDao.insertRisk(localResult.toEntity(
                syncStatus = dev.abbasian.data.local.entity.SyncStatus.PENDING,
                appVersion = appPackage.versionCode,
                lastUpdateTime = appPackage.lastUpdateTime
            ))
            return localResult
        }

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

    override suspend fun syncScanLogsToCloud(appPackages: List<AppPackage>): Int {
        if (appPackages.isEmpty()) return 0

        var synced = 0
        appPackages.chunked(CLOUD_BATCH_SIZE).forEach { chunk ->
            try {
                val dtos = chunk.map { buildMetadataDto(it) }
                api.batchScan(dev.abbasian.data.remote.dto.BatchScanRequestDto(dtos))
                synced += chunk.size
                Log.i(TAG, "Batch synced ${chunk.size} apps to cloud ($synced/${appPackages.size})")
            } catch (e: Exception) {
                Log.e(TAG, "Batch cloud sync failed for ${chunk.size} apps", e)
            }
        }
        return synced
    }

    override suspend fun scanApps(appPackages: List<AppPackage>): List<RiskAssessment> {
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
                Log.e(TAG, "Batch scan failed, falling back to sequential", e)
            }
        }
        return appPackages.map { scanApp(it, lowSpeedMode = false, syncToCloud = true) }
    }

    private suspend fun reportToCloud(
        appPackage: AppPackage,
        localResult: RiskAssessment?
    ): RiskAssessment? {
        return try {
            api.analyzeApp(buildMetadataDto(appPackage, localResult)).toRiskAssessment()
        } catch (e: Exception) {
            Log.w(TAG, "Cloud report failed for ${appPackage.packageName}: ${e.message}")
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

    private companion object {
        const val TAG = "RaybodCloud"
        const val CLOUD_BATCH_SIZE = 25
    }
}
