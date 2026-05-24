package dev.abbasian.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abbasian.agent.scanner.PackageAnalyzer
import dev.abbasian.domain.model.RiskAssessment
import dev.abbasian.domain.model.RiskLevel
import dev.abbasian.domain.usecase.ScanAppUseCase
import dev.abbasian.domain.repository.UserPreferencesRepository
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
    val error: String? = null
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanAppUseCase: ScanAppUseCase,
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
            
            val apps = packageAnalyzer.getInstalledApps() // This is heavy, should be background
            val total = apps.size
            val results = mutableListOf<RiskAssessment>()

            _uiState.value = _uiState.value.copy(totalApps = total)

            apps.forEachIndexed { index, app ->
                // Check if the coroutine is still active (not cancelled)
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
                
                // Actual scan (calls Cloud or Cache)
                val result = scanAppUseCase(app)
                results.add(result)
                
                // Update UI state with latest results incrementally
                _uiState.value = _uiState.value.copy(
                    results = results.toList()
                )
                
                // Add delay in low-speed mode for better performance
                if (lowSpeedMode && isActive) {
                    delay(LOW_SPEED_DELAY_MS)
                }
            }

            // Only update to complete if scan wasn't cancelled
            if (isActive) {
                // Sort results: Critical > High > Medium > Low > Safe > Unknown
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
                    results = sortedResults
                )
                
                val newTimestamp = System.currentTimeMillis()
                userPrefs.setLastScanTimestamp(newTimestamp)
                userPrefs.setDailyScanCount(if (isSameDay(lastScan, newTimestamp)) currentDayCount + 1 else 1)
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
