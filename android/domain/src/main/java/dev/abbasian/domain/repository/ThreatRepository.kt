package dev.abbasian.domain.repository

import dev.abbasian.domain.model.AppPackage
import dev.abbasian.domain.model.RiskAssessment

interface ThreatRepository {
    /**
     * Scans a single application package and returns a risk assessment.
     * @param syncToCloud When false, skips per-app cloud POST (use syncScanLogsToCloud after full scan).
     */
    suspend fun scanApp(
        appPackage: AppPackage,
        lowSpeedMode: Boolean = false,
        syncToCloud: Boolean = true
    ): RiskAssessment

    /**
     * Scans a list of application packages.
     */
    suspend fun scanApps(appPackages: List<AppPackage>): List<RiskAssessment>

    /**
     * Sends scan metadata to the cloud in batches so the admin analytics panel is updated.
     * Returns the number of packages successfully reported.
     */
    suspend fun syncScanLogsToCloud(appPackages: List<AppPackage>): Int
}
