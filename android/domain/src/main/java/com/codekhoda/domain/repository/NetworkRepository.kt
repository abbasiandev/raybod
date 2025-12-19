package com.codekhoda.domain.repository

import com.codekhoda.domain.model.NetworkAlert
import com.codekhoda.domain.model.NetworkFlow
import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    fun getActiveFlows(): Flow<List<NetworkFlow>>
    fun getNetworkAlerts(): Flow<List<NetworkAlert>>
    suspend fun analyzeFlows(flows: List<NetworkFlow>)
    suspend fun getLocalBlocklist(): List<String>
    suspend fun syncBlocklist()
}


