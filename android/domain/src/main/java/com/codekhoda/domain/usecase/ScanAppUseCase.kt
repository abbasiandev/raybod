package com.codekhoda.domain.usecase

import com.codekhoda.domain.model.AppPackage
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.repository.ThreatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ScanAppUseCase @Inject constructor(
    private val threatRepository: ThreatRepository
) {
    suspend operator fun invoke(appPackage: AppPackage): RiskAssessment {
        return withContext(Dispatchers.Default) {
            threatRepository.scanApp(appPackage)
        }
    }
}
