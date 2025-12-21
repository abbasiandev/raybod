package com.codekhoda.data.repository

import com.codekhoda.data.local.dao.NetworkDao
import com.codekhoda.data.local.entity.BlocklistEntity
import com.codekhoda.data.local.entity.NetworkAlertEntity
import com.codekhoda.data.remote.api.CloudBrainApi
import com.codekhoda.data.remote.dto.NetworkAnalysisRequestDto
import com.codekhoda.data.remote.dto.NetworkFlowDto
import com.codekhoda.domain.model.NetworkAlert
import com.codekhoda.domain.model.NetworkFlow
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    private val networkDao: NetworkDao,
    private val api: CloudBrainApi
) : NetworkRepository {

    private val _activeFlows = MutableStateFlow<List<NetworkFlow>>(emptyList())
    
    override fun getActiveFlows(): Flow<List<NetworkFlow>> = _activeFlows.asStateFlow()

    override fun getNetworkAlerts(): Flow<List<NetworkAlert>> = networkDao.getAllAlerts().map { alertEntities ->
        alertEntities.map { alertEntity ->
            NetworkAlert(
                id = alertEntity.id,
                packageName = alertEntity.packageName,
                destination = alertEntity.destination,
                threatType = alertEntity.threatType,
                riskLevel = RiskLevel.valueOf(alertEntity.riskLevel),
                description = alertEntity.description,
                timestamp = alertEntity.timestamp
            )
        }
    }

    override suspend fun analyzeFlows(flows: List<NetworkFlow>) {
        // Update active flows for real-time UI display
        _activeFlows.value = flows

        // Send flows to cloud for threat analysis
        try {
            val analysisRequest = NetworkAnalysisRequestDto(
                flows = flows.map { networkFlow ->
                    NetworkFlowDto(
                        sourceApp = networkFlow.sourceApp,
                        destinationIp = networkFlow.destinationIp,
                        destinationPort = networkFlow.destinationPort,
                        protocol = networkFlow.protocol,
                        domain = networkFlow.domain,
                        bytesSent = networkFlow.bytesSent,
                        bytesReceived = networkFlow.bytesReceived,
                        timestamp = networkFlow.timestamp
                    )
                }
            )
            
            val analysisResult = api.analyzeNetwork(analysisRequest)
            
            // Store detected network threats in local database
            analysisResult.alerts.forEach { alertDto ->
                networkDao.insertAlert(
                    NetworkAlertEntity(
                        packageName = alertDto.packageName,
                        destination = alertDto.destination,
                        threatType = alertDto.threatType,
                        riskLevel = alertDto.riskLevel,
                        description = alertDto.description,
                        timestamp = alertDto.timestamp
                    )
                )
            }
            
            // Update local blocklist with cloud-provided patterns
            if (analysisResult.blocklist.isNotEmpty()) {
                networkDao.insertBlocklist(analysisResult.blocklist.map { blocklistDto ->
                    BlocklistEntity(
                        pattern = blocklistDto.pattern,
                        type = blocklistDto.type,
                        reason = blocklistDto.reason,
                        timestamp = blocklistDto.timestamp
                    )
                })
            }
        } catch (e: Exception) {
            // Handle offline mode gracefully - local analysis still works
        }
    }

    override suspend fun getLocalBlocklist(): List<String> {
        return networkDao.getBlocklistPatterns()
    }

    override suspend fun syncBlocklist() {
        // Blocklist sync is handled automatically during flow analysis
    }

    override fun clearActiveFlows() {
        _activeFlows.value = emptyList()
    }
}


