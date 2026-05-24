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
import dev.abbasian.data.debug.DebugTrace
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
        var chunkIndex = 0
        var localHighThreatCount = 0
        appPackages.forEach { app ->
            val cached = riskDao.getRisk(app.packageName)
            if (cached != null) {
                val level = try {
                    RiskLevel.valueOf(cached.riskLevel)
                } catch (_: Exception) {
                    RiskLevel.UNKNOWN
                }
                if (level == RiskLevel.HIGH || level == RiskLevel.CRITICAL || level == RiskLevel.MEDIUM) {
                    localHighThreatCount++
                }
            }
        }
        // #region agent log
        DebugTrace.log(
            hypothesisId = "A,E",
            location = "ThreatRepositoryImpl.kt:syncScanLogsToCloud:start",
            message = "Starting batch cloud sync",
            data = mapOf(
                "totalApps" to appPackages.size,
                "chunkSize" to CLOUD_BATCH_SIZE,
                "localThreatCount" to localHighThreatCount
            )
        )
        // #endregion
        appPackages.chunked(CLOUD_BATCH_SIZE).forEach { chunk ->
            chunkIndex++
            var chunkWithEnsemble = 0
            var chunkLocalThreats = 0
            var ensembleSentInPayload = 0
            val dtos = chunk.map { app ->
                val cached = riskDao.getRisk(app.packageName)?.toDomain()
                if (cached?.ensembleMetadata?.isNotEmpty() == true) {
                    chunkWithEnsemble++
                }
                if (cached != null && cached.riskLevel != RiskLevel.SAFE && cached.riskLevel != RiskLevel.UNKNOWN) {
                    chunkLocalThreats++
                }
                val dto = buildBatchSyncDto(app, cached)
                if (dto.ensembleMetadata?.isNotEmpty() == true) {
                    ensembleSentInPayload++
                }
                dto
            }
            // #region agent log
            DebugTrace.log(
                hypothesisId = "C",
                location = "ThreatRepositoryImpl.kt:syncScanLogsToCloud:chunk",
                message = "Prepared batch chunk payload",
                data = mapOf(
                    "chunkIndex" to chunkIndex,
                    "chunkApps" to chunk.size,
                    "localThreatsInChunk" to chunkLocalThreats,
                    "withEnsembleMetadataAvailable" to chunkWithEnsemble,
                    "ensembleSentInPayload" to ensembleSentInPayload
                )
            )
            // #endregion
            try {
                api.batchScan(dev.abbasian.data.remote.dto.BatchScanRequestDto(dtos))
                synced += chunk.size
                Log.i(TAG, "Batch synced ${chunk.size} apps to cloud ($synced/${appPackages.size})")
                // #region agent log
                DebugTrace.log(
                    hypothesisId = "A",
                    location = "ThreatRepositoryImpl.kt:syncScanLogsToCloud:success",
                    message = "Batch chunk synced successfully",
                    data = mapOf(
                        "chunkIndex" to chunkIndex,
                        "syncedTotal" to synced,
                        "targetTotal" to appPackages.size
                    )
                )
                // #endregion
            } catch (e: Exception) {
                Log.e(TAG, "Batch cloud sync failed for ${chunk.size} apps", e)
                // #region agent log
                DebugTrace.log(
                    hypothesisId = "A",
                    location = "ThreatRepositoryImpl.kt:syncScanLogsToCloud:failure",
                    message = "Batch chunk sync failed",
                    data = describeSyncError(e) + mapOf("chunkIndex" to chunkIndex)
                )
                // #endregion
            }
        }
        // #region agent log
        DebugTrace.log(
            hypothesisId = "A,E",
            location = "ThreatRepositoryImpl.kt:syncScanLogsToCloud:done",
            message = "Batch cloud sync finished",
            data = mapOf(
                "synced" to synced,
                "totalApps" to appPackages.size,
                "localThreatCount" to localHighThreatCount
            )
        )
        // #endregion
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

    private fun buildBatchSyncDto(
        appPackage: AppPackage,
        localResult: RiskAssessment?
    ): AppMetadataDto {
        return AppMetadataDto(
            packageName = appPackage.packageName,
            versionCode = appPackage.versionCode,
            signature = appPackage.signature,
            permissions = emptyList(),
            ensembleMetadata = sanitizeEnsembleMetadata(localResult?.ensembleMetadata),
            intents = emptyList(),
            versionName = appPackage.versionName,
            installTime = appPackage.installTime,
            lastUpdateTime = appPackage.lastUpdateTime,
            hasReflection = appPackage.hasReflection,
            hasDynamicLoading = appPackage.hasDynamicLoading
        )
    }

    private fun sanitizeEnsembleMetadata(
        metadata: Map<String, Float>?
    ): Map<String, Float>? {
        if (metadata.isNullOrEmpty()) return null
        val sanitized = metadata.mapNotNull { (key, value) ->
            if (value.isFinite() && value >= 0f) key to value else null
        }.toMap()
        return sanitized.ifEmpty { null }
    }

    private fun describeSyncError(e: Exception): Map<String, Any?> {
        val details = mutableMapOf<String, Any?>(
            "errorType" to e.javaClass.simpleName,
            "errorMessage" to (e.message ?: "unknown")
        )
        var cause = e.cause
        var depth = 0
        while (cause != null && depth < 3) {
            details["cause$depth"] = "${cause.javaClass.simpleName}: ${cause.message}"
            cause = cause.cause
            depth++
        }
        if (e is retrofit2.HttpException) {
            details["httpCode"] = e.code()
            details["httpBody"] = e.response()?.errorBody()?.string()?.take(200)
        }
        return details
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
            ensembleMetadata = sanitizeEnsembleMetadata(localResult?.ensembleMetadata),
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
        const val CLOUD_BATCH_SIZE = 10
    }
}
