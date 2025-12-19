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

    override fun getNetworkAlerts(): Flow<List<NetworkAlert>> = networkDao.getAllAlerts().map { entities ->
        entities.map { entity ->
            NetworkAlert(
                id = entity.id,
                packageName = entity.packageName,
                destination = entity.destination,
                threatType = entity.threatType,
                riskLevel = RiskLevel.valueOf(entity.riskLevel),
                description = entity.description,
                timestamp = entity.timestamp
            )
        }
    }

    override suspend fun analyzeFlows(flows: List<NetworkFlow>) {
        // Update active flows for UI
        _activeFlows.value = flows

        // Send to cloud for analysis
        try {
            val dto = NetworkAnalysisRequestDto(
                flows = flows.map { flow ->
                    NetworkFlowDto(
                        sourceApp = flow.sourceApp,
                        destinationIp = flow.destinationIp,
                        destinationPort = flow.destinationPort,
                        protocol = flow.protocol,
                        domain = flow.domain,
                        bytesSent = flow.bytesSent,
                        bytesReceived = flow.bytesReceived,
                        timestamp = flow.timestamp
                    )
                }
            )
            
            val result = api.analyzeNetwork(dto)
            
            // Save alerts
            result.alerts.forEach { alertDto ->
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
            
            // Update blocklist if provided
            if (result.blocklist.isNotEmpty()) {
                networkDao.insertBlocklist(result.blocklist.map { 
                    BlocklistEntity(
                        pattern = it.pattern,
                        type = it.type,
                        reason = it.reason,
                        timestamp = it.timestamp
                    )
                })
            }
        } catch (e: Exception) {
            // Log error or handle offline mode
        }
    }

    override suspend fun getLocalBlocklist(): List<String> {
        return networkDao.getBlocklistPatterns()
    }

    override suspend fun syncBlocklist() {
        // Implement periodic sync if needed or handle via analyzeFlows
    }
}


