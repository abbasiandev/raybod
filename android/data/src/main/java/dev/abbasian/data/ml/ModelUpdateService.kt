package dev.abbasian.data.ml

import dev.abbasian.data.remote.api.CloudBrainApi
import dev.abbasian.data.remote.dto.ModelVersionDto
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

    suspend fun getRemotePackageLists(): Map<String, List<String>>? {
        return try {
            api.getRemotePackageLists()
        } catch (e: Exception) {
            null
        }
    }
}

