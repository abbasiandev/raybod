package dev.abbasian.domain.usecase

import dev.abbasian.domain.model.AppPackage
import dev.abbasian.domain.repository.ThreatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SyncScanLogsUseCase @Inject constructor(
    private val threatRepository: ThreatRepository
) {
    suspend operator fun invoke(appPackages: List<AppPackage>): Int {
        return withContext(Dispatchers.IO) {
            threatRepository.syncScanLogsToCloud(appPackages)
        }
    }
}
