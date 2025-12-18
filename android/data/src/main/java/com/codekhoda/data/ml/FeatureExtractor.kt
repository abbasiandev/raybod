package com.codekhoda.data.ml

import android.content.Context
import com.codekhoda.domain.model.AppPackage
import dagger.hilt.android.qualifiers.ApplicationContext
import com.codekhoda.data.ml.FeatureAnalysisResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class to hold both the raw vector for TFLite and the human-readable features for UI.
 */
data class FeatureAnalysisResult(
    val featureVector: FloatArray,
    val matchedFeatures: List<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeatureAnalysisResult

        if (!featureVector.contentEquals(other.featureVector)) return false
        if (matchedFeatures != other.matchedFeatures) return false

        return true
    }

    override fun hashCode(): Int {
        var result = featureVector.contentHashCode()
        result = 31 * result + matchedFeatures.hashCode()
        return result
    }
}

@Singleton
class FeatureExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val permissionFeatures: List<String>
    private val intentFeatures: List<String>
    val vectorSize: Int

    init {
        val json = loadJSONFromAsset("features.json")
        var pList = mutableListOf<String>()
        var iList = mutableListOf<String>()
        
        try {
            val gson = Gson()
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            val data: Map<String, List<String>> = gson.fromJson(json, type)
            
            pList = data["permissions"]?.toMutableList() ?: mutableListOf()
            iList = data["intents"]?.toMutableList() ?: mutableListOf()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        permissionFeatures = pList
        intentFeatures = iList
        
        // Ensure vector size matches model expectation
        vectorSize = 2000
    }

    fun extractFeatures(appPackage: AppPackage): FeatureAnalysisResult {
        val vector = FloatArray(vectorSize) { 0f }
        val matchedFeatures = mutableListOf<String>()
        
        // 1. Map Permissions
        // Reference uses direct index mapping
        permissionFeatures.forEachIndexed { index, feature ->
            if (appPackage.permissions.contains(feature)) {
                if (index < vectorSize) {
                    vector[index] = 1f
                    matchedFeatures.add("Permission: $feature")
                }
            }
        }
        
        // 2. Map Intents
        // Reference code: inputVal[i + 489] = 1; where 489 is likely p_jArray.length()
        // We will use permissionFeatures.size as the offset to be dynamic but logically consistent
        val intentOffset = permissionFeatures.size // Should be 489 if features.json is same
        
        intentFeatures.forEachIndexed { index, feature ->
            if (appPackage.intents.contains(feature)) {
                val targetIndex = intentOffset + index
                if (targetIndex < vectorSize) {
                    vector[targetIndex] = 1f
                    matchedFeatures.add("Intent: $feature")
                }
            }
        }
        
        return FeatureAnalysisResult(vector, matchedFeatures)
    }

    private fun loadJSONFromAsset(filename: String): String {
        return try {
            val inputStream: InputStream = context.assets.open(filename)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (e: Exception) {
            e.printStackTrace()
            "{}"
        }
    }
}
