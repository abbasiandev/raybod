package dev.abbasian.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abbasian.agent.scanner.PackageAnalyzer
import dev.abbasian.domain.model.RiskAssessment
import dev.abbasian.domain.model.RiskLevel
import dev.abbasian.domain.usecase.ScanAppUseCase
import dev.abbasian.domain.usecase.SyncScanLogsUseCase
import dev.abbasian.domain.repository.UserPreferencesRepository
import dev.abbasian.agent.worker.CloudSyncScheduler
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val currentApp: String = "",
    val currentAppLabel: String = "",
    val totalApps: Int = 0,
    val scannedApps: Int = 0,
    val recentApps: List<Pair<String, String>> = emptyList(), // Pair<PackageName, AppLabel>
    val results: List<RiskAssessment> = emptyList(),
    val showResultsSheet: Boolean = false,
    val isLowSpeedMode: Boolean = false,
    val isRooted: Boolean = false,
    val isEmulator: Boolean = false,
    val cloudSyncMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val scanAppUseCase: ScanAppUseCase,
    private val syncScanLogsUseCase: SyncScanLogsUseCase,
    private val packageAnalyzer: PackageAnalyzer,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState = _uiState.asStateFlow()
    
    private companion object {
        const val FREEMIUM_DAILY_SCAN_LIMIT = 3
        const val LOW_SPEED_DELAY_MS = 500L
        const val MAX_RECENT_APPS_DISPLAY = 5
    }
    
    init {
        checkIntegrity()
    }

    private fun checkIntegrity() {
        viewModelScope.launch {
            val isRooted = dev.abbasian.agent.util.DeviceIntegrityChecker.isRooted()
            val isEmulator = dev.abbasian.agent.util.DeviceIntegrityChecker.isEmulator()
            _uiState.value = _uiState.value.copy(
                isRooted = isRooted,
                isEmulator = isEmulator
            )
        }
    }
    
    private var scanJob: Job? = null

    fun startScan(lowSpeedMode: Boolean = false) {
        // Cancel any existing scan
        scanJob?.cancel()
        
        scanJob = viewModelScope.launch {
            val plan = userPrefs.userPlan.first()
            val lastScan = userPrefs.lastScanTimestamp.first()
            val scanCount = userPrefs.dailyScanCount.first()
            val currentTime = System.currentTimeMillis()
            
            // Check if it's a new day (reset count)
            val isNewDay = !isSameDay(lastScan, currentTime)
            val currentDayCount = if (isNewDay) 0 else scanCount
            
            // Limit Freemium to specified number of scans per day
            if (plan == "FREEMIUM" && currentDayCount >= FREEMIUM_DAILY_SCAN_LIMIT) {
                _uiState.value = _uiState.value.copy(
                    error = "Daily scan limit reached ($FREEMIUM_DAILY_SCAN_LIMIT/$FREEMIUM_DAILY_SCAN_LIMIT). Upgrade to Premium for unlimited protection!"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isScanning = true, 
                progress = 0f, 
                results = emptyList(),
                isLowSpeedMode = lowSpeedMode,
                error = null,
                totalApps = 0,
                scannedApps = 0,
                recentApps = emptyList(),
                currentApp = "",
                currentAppLabel = "Preparing..."
            )

            try {
                val apps = packageAnalyzer.getInstalledApps()
                val total = apps.size.coerceAtLeast(1)
                val results = mutableListOf<RiskAssessment>()

                _uiState.value = _uiState.value.copy(totalApps = apps.size)

                apps.forEachIndexed { index, app ->
                    if (!isActive) {
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            currentApp = "Scan Stopped",
                            currentAppLabel = "Cancelled"
                        )
                        return@launch
                    }

                    val currentRecentApps = (_uiState.value.recentApps + (app.packageName to app.appLabel))
                        .takeLast(MAX_RECENT_APPS_DISPLAY)

                    _uiState.value = _uiState.value.copy(
                        progress = index / total.toFloat(),
                        currentApp = app.packageName,
                        currentAppLabel = app.appLabel,
                        scannedApps = index + 1,
                        recentApps = currentRecentApps
                    )

                    val result = scanAppUseCase(app, lowSpeedMode, syncToCloud = false)
                    results.add(result)

                    _uiState.value = _uiState.value.copy(
                        results = results.toList()
                    )

                    if (lowSpeedMode && isActive) {
                        delay(LOW_SPEED_DELAY_MS)
                    }
                }

                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        currentAppLabel = "Syncing to cloud..."
                    )

                    val syncedCount = syncScanLogsUseCase(apps)
                    val localThreatCount = results.count { result ->
                        result.riskLevel != RiskLevel.SAFE && result.riskLevel != RiskLevel.UNKNOWN
                    }
                    // #region agent log
                    android.util.Log.i(
                        "RaybodDebug",
                        org.json.JSONObject(
                            mapOf(
                                "sessionId" to "e7b765",
                                "hypothesisId" to "A,C,D,E",
                                "location" to "ScanViewModel.kt:startScan:postSync",
                                "message" to "Scan finished and cloud sync attempted",
                                "timestamp" to System.currentTimeMillis(),
                                "runId" to "post-fix",
                                "data" to org.json.JSONObject(
                                    mapOf(
                                        "syncedCount" to syncedCount,
                                        "totalApps" to apps.size,
                                        "localThreatCount" to localThreatCount,
                                        "lowSpeedMode" to lowSpeedMode
                                    )
                                )
                            )
                        ).toString()
                    )
                    // #endregion
                    val cloudSyncMessage = when {
                        apps.isEmpty() -> null
                        syncedCount >= apps.size -> "Reported $syncedCount/${apps.size} apps to cloud"
                        syncedCount > 0 -> "Reported $syncedCount/${apps.size} apps to cloud (retry scheduled)"
                        else -> "Cloud sync failed — retry scheduled"
                    }

                    if (syncedCount < apps.size) {
                        CloudSyncScheduler.enqueueRetry(
                            appContext,
                            apps.map { it.packageName }
                        )
                    }

                    val sortedResults = results.sortedByDescending { result ->
                        when (result.riskLevel) {
                            RiskLevel.CRITICAL -> 5
                            RiskLevel.HIGH -> 4
                            RiskLevel.MEDIUM -> 3
                            RiskLevel.LOW -> 2
                            RiskLevel.SAFE -> 1
                            RiskLevel.UNKNOWN -> 0
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        progress = 1f,
                        currentApp = "Scan Complete",
                        currentAppLabel = "Finished",
                        results = sortedResults,
                        cloudSyncMessage = cloudSyncMessage
                    )

                    val newTimestamp = System.currentTimeMillis()
                    userPrefs.setLastScanTimestamp(newTimestamp)
                    userPrefs.setDailyScanCount(if (isSameDay(lastScan, newTimestamp)) currentDayCount + 1 else 1)
                }
            } catch (e: Exception) {
                android.util.Log.e("ScanViewModel", "Scan failed", e)
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = "Scan failed: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
    
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            currentApp = "Scan Stopped",
            currentAppLabel = "Cancelled"
        )
    }
    
    fun toggleLowSpeedMode() {
        _uiState.value = _uiState.value.copy(
            isLowSpeedMode = !_uiState.value.isLowSpeedMode
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun setShowResultsSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showResultsSheet = show)
    }
}
