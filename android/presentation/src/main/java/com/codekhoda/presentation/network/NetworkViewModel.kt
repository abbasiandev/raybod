package com.codekhoda.presentation.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codekhoda.domain.model.NetworkAlert
import com.codekhoda.domain.model.NetworkFlow
import com.codekhoda.domain.repository.NetworkRepository
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

    val activeFlows: StateFlow<List<NetworkFlow>> = repository.getActiveFlows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkAlerts: StateFlow<List<NetworkAlert>> = repository.getNetworkAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setVpnActive(active: Boolean) {
        _isVpnActive.value = active
    }

    fun simulateMaliciousFlow() {
        viewModelScope.launch {
            val maliciousFlow = NetworkFlow(
                id = UUID.randomUUID().toString(),
                sourceApp = "com.suspicious.app",
                destinationIp = "1.2.3.4",
                destinationPort = 1337,
                protocol = "TCP",
                domain = "malicious-c2-asdfghjkl.com",
                bytesSent = 5000,
                bytesReceived = 0,
                timestamp = System.currentTimeMillis()
            )
            repository.analyzeFlows(listOf(maliciousFlow))
        }
    }
}
