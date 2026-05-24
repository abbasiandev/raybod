package dev.abbasian.presentation.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abbasian.domain.model.NetworkAlert
import dev.abbasian.domain.model.NetworkFlow
import dev.abbasian.domain.repository.NetworkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val repository: NetworkRepository
) : ViewModel() {

    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private companion object {
        const val SIMULATION_INTERVAL_MS = 3000L
        const val FLOW_CACHE_TIMEOUT_MS = 5000L
        const val SIMULATED_MALICIOUS_PORT = 1337
        const val SIMULATED_BYTES_SENT = 5000L
    }

    val activeFlows: StateFlow<List<NetworkFlow>> = repository.getActiveFlows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkAlerts: StateFlow<List<NetworkAlert>> = repository.getNetworkAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setVpnActive(active: Boolean) {
        _isVpnActive.value = active
        if (!active) {
            _isSimulating.value = false
        }
    }

    fun toggleSimulation() {
        _isSimulating.value = !_isSimulating.value
        if (_isSimulating.value) {
            startSimulationLoop()
        }
    }

    private fun startSimulationLoop() {
        viewModelScope.launch {
            while (_isSimulating.value) {
                simulateMaliciousFlow()
                kotlinx.coroutines.delay(SIMULATION_INTERVAL_MS)
            }
        }
    }

    /**
     * Simulates a malicious network flow for demo/testing purposes.
     * Creates a suspicious connection to a known bad IP/domain pattern.
     */
    fun simulateMaliciousFlow() {
        viewModelScope.launch {
            val simulatedMaliciousFlow = NetworkFlow(
                id = UUID.randomUUID().toString(),
                sourceApp = "com.suspicious.app",
                destinationIp = "1.2.3.4",
                destinationPort = SIMULATED_MALICIOUS_PORT,
                protocol = "TCP",
                domain = "malicious-c2-asdfghjkl.com",
                bytesSent = SIMULATED_BYTES_SENT,
                bytesReceived = 0,
                timestamp = System.currentTimeMillis()
            )
            repository.analyzeFlows(listOf(simulatedMaliciousFlow))
        }
    }
}
