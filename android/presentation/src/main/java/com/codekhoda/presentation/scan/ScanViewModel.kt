package com.codekhoda.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codekhoda.agent.scanner.PackageAnalyzer
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.usecase.ScanAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val currentApp: String = "",
    val results: List<RiskAssessment> = emptyList()
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanAppUseCase: ScanAppUseCase,
    private val packageAnalyzer: PackageAnalyzer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState = _uiState.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, progress = 0f, results = emptyList())
            
            val apps = packageAnalyzer.getInstalledApps() // This is heavy, should be background
            val total = apps.size
            val results = mutableListOf<RiskAssessment>()

            apps.forEachIndexed { index, app ->
                _uiState.value = _uiState.value.copy(
                    progress = index / total.toFloat(),
                    currentApp = app.packageName
                )
                
                // Actual scan (calls Cloud or Cache)
                val result = scanAppUseCase(app)
                results.add(result)
            }

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                progress = 1f,
                currentApp = "Scan Complete",
                results = results
            )
        }
    }
}
