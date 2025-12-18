package com.codekhoda.data.ml

import android.content.Context
import com.codekhoda.domain.model.AppPackage
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

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

    fun extractFeatures(appPackage: AppPackage): FloatArray {
        val vector = FloatArray(vectorSize) { 0f }
        
        // 1. Map Permissions
        // Reference uses direct index mapping
        permissionFeatures.forEachIndexed { index, feature ->
            if (appPackage.permissions.contains(feature)) {
                if (index < vectorSize) {
                    vector[index] = 1f
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
                }
            }
        }
        
        return vector
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
