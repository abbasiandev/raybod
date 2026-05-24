package dev.abbasian.domain.repository

import dev.abbasian.domain.model.AppPackage
import dev.abbasian.domain.model.RiskAssessment

interface ThreatRepository {
    /**
     * Scans a single application package and returns a risk assessment.
     */
    suspend fun scanApp(appPackage: AppPackage, lowSpeedMode: Boolean = false): RiskAssessment


    /**
     * Scans a list of application packages.
     */
    suspend fun scanApps(appPackages: List<AppPackage>): List<RiskAssessment>
}
