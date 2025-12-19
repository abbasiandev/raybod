package com.codekhoda.presentation.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codekhoda.domain.model.NetworkAlert
import com.codekhoda.domain.model.NetworkFlow
import com.codekhoda.domain.repository.NetworkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val repository: NetworkRepository
) : ViewModel() {

    val activeFlows: StateFlow<List<NetworkFlow>> = repository.getActiveFlows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkAlerts: StateFlow<List<NetworkAlert>> = repository.getNetworkAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}


