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

    override suspend fun scanApp(appPackage: AppPackage): RiskAssessment {
        // 1. Check local cache
        val cached = riskDao.getRisk(appPackage.packageName)
        if (cached != null) {
            return cached.toDomain()
        }

        // 2. Perform On-Device AI Scan (First Line of Defense)
        val localResult = malwareScanner.scan(appPackage)
        
        // If critical, return immediately (Blocking threat)
        if (localResult.riskLevel == RiskLevel.CRITICAL) {
             riskDao.insertRisk(localResult.toEntity())
             return localResult
        }

        // 3. Call Cloud API for Second Opinion (if not critical)
        val finalResult = try {
            val dto = com.codekhoda.data.remote.dto.AppMetadataDto(
                packageName = appPackage.packageName,
                versionCode = appPackage.versionCode,
                signature = appPackage.signature,
                permissions = appPackage.permissions
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
            // Fallback to local AI result if cloud fails
            localResult
        }

        // 4. Cache result
        riskDao.insertRisk(finalResult.toEntity())

        return finalResult
    }

    override suspend fun scanApps(appPackages: List<AppPackage>): List<RiskAssessment> {
        return appPackages.map { scanApp(it) }
    }
}
