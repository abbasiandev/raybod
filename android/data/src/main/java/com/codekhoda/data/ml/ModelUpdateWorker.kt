package com.codekhoda.data.ml

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class ModelUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val modelUpdateService: ModelUpdateService,
    private val malwareScanner: MalwareScanner
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ModelUpdateWorker"
        private const val PREFS_NAME = "sentinel_prefs"
        private const val PREF_KEY_MODEL_VERSION = "model_version"
        private const val DEFAULT_MODEL_VERSION = "1.0.0"
        private const val MODEL_FILENAME = "updated_model.tflite"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting background sync...")
        
        // 1. Update Package Lists (Whitelist/Blacklist)
        try {
            val remoteLists = modelUpdateService.getRemotePackageLists()
            if (remoteLists != null) {
                val whitelist = remoteLists["whitelist"]?.toSet()
                if (whitelist != null) {
                    com.codekhoda.domain.filter.SystemPackageFilter.updateExcludedPackages(whitelist)
                    Log.d("ModelUpdateWorker", "Remote whitelist updated with ${whitelist.size} packages")
                }
            }
        } catch (e: Exception) {
            Log.e("ModelUpdateWorker", "Failed to update package lists: ${e.message}")
        }

        // 2. Update ML Model
        val sharedPrefs = applicationContext.getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
        val currentVersion = sharedPrefs.getString("model_version", "1.0.0") ?: "1.0.0"
        
        try {
            val modelInfo = modelUpdateService.getCurrentModelInfo()
            if (modelInfo == null) {
                Log.e("ModelUpdateWorker", "Failed to fetch model info from cloud")
                return Result.retry()
            }
            
            if (modelInfo.version == currentVersion) {
                Log.d("ModelUpdateWorker", "Model is up to date: $currentVersion")
                return Result.success()
            }
            
            Log.d("ModelUpdateWorker", "New model version found: ${modelInfo.version}. Downloading...")
            
            val modelBytes = modelUpdateService.downloadModel(modelInfo.version)
            if (modelBytes == null) {
                Log.e("ModelUpdateWorker", "Failed to download model bytes")
                return Result.retry()
            }
            
            // Save model to internal storage
            val modelFile = File(applicationContext.filesDir, "updated_model.tflite")
            FileOutputStream(modelFile).use { it.write(modelBytes) }
            
            // Update version in prefs
            sharedPrefs.edit().putString("model_version", modelInfo.version).apply()
            
            // Reload scanner with new model
            malwareScanner.reloadModel()
            
            Log.d("ModelUpdateWorker", "Successfully updated model to ${modelInfo.version}")
            return Result.success()
            
        } catch (e: Exception) {
            Log.e("ModelUpdateWorker", "Error during model update: ${e.message}")
            return Result.retry()
        }
    }
}

