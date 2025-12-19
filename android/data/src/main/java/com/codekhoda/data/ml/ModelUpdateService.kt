package com.codekhoda.data.ml

import com.codekhoda.data.remote.api.CloudBrainApi
import com.codekhoda.data.remote.dto.ModelVersionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelUpdateService @Inject constructor(
    private val api: CloudBrainApi
) {
    suspend fun getCurrentModelInfo(): ModelVersionDto? {
        return try {
            api.getCurrentModel()
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun downloadModel(version: String): ByteArray? {
        return try {
            api.downloadModel(version).bytes()
        } catch (e: Exception) {
            null
        }
    }
}

