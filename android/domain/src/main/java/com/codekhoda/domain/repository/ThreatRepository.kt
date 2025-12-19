package com.codekhoda.domain.repository

import com.codekhoda.domain.model.AppPackage
import com.codekhoda.domain.model.RiskAssessment

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
