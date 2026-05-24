package dev.abbasian.domain.repository

import dev.abbasian.domain.model.NetworkAlert
import dev.abbasian.domain.model.NetworkFlow
import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    fun getActiveFlows(): Flow<List<NetworkFlow>>
    fun getNetworkAlerts(): Flow<List<NetworkAlert>>
    suspend fun analyzeFlows(flows: List<NetworkFlow>)
    suspend fun getLocalBlocklist(): List<String>
    suspend fun syncBlocklist()
    fun clearActiveFlows()
}


