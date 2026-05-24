package dev.abbasian.domain.usecase

import dev.abbasian.domain.model.AppPackage
import dev.abbasian.domain.model.RiskAssessment
import dev.abbasian.domain.repository.ThreatRepository
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
