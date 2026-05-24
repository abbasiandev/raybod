package dev.abbasian.data.ml

import android.content.Context
import android.content.res.AssetManager
import dev.abbasian.domain.model.AppPackage
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class FeatureExtractorTest {

    private lateinit var context: Context
    private lateinit var assetManager: AssetManager
    private lateinit var featureExtractor: FeatureExtractor

    @Before
    fun setup() {
        context = mockk()
        assetManager = mockk()
        
        val featuresJson = """
            {
                "permissions": ["android.permission.INTERNET", "android.permission.CAMERA"],
                "intents": ["android.intent.action.MAIN", "android.intent.action.BOOT_COMPLETED"]
            }
        """.trimIndent()
        
        every { context.assets } returns assetManager
        every { assetManager.open("features.json") } returns ByteArrayInputStream(featuresJson.toByteArray())
        
        featureExtractor = FeatureExtractor(context)
    }

    @Test
    fun `vector size is 2000`() {
        assertEquals(2000, featureExtractor.vectorSize)
    }

    @Test
    fun `extractFeatures sets correct bits for permissions`() {
        val appPackage = AppPackage(
            packageName = "com.test",
            versionCode = 1,
            signature = "sig",
            permissions = listOf("android.permission.INTERNET"), // Only 1st feature
            intents = emptyList()
        )
        
        val vector = featureExtractor.extractFeatures(appPackage).featureVector
        
        assertEquals(1f, vector[0], 0.0f) // INTERNET
        assertEquals(0f, vector[1], 0.0f) // CAMERA
    }

    @Test
    fun `extractFeatures sets correct bits for intents`() {
        val appPackage = AppPackage(
            packageName = "com.test",
            versionCode = 1,
            signature = "sig",
            permissions = emptyList(),
            intents = listOf("android.intent.action.BOOT_COMPLETED") // Only 2nd intent
        )
        
        val vector = featureExtractor.extractFeatures(appPackage).featureVector
        
        // Offset for intents is permissionFeatures.size (2 in this test)
        assertEquals(0f, vector[2], 0.0f) // MAIN
        assertEquals(1f, vector[3], 0.0f) // BOOT_COMPLETED
    }

    @Test
    fun `extractFeatures handles multiple features`() {
        val appPackage = AppPackage(
            packageName = "com.test",
            versionCode = 1,
            signature = "sig",
            permissions = listOf("android.permission.INTERNET", "android.permission.CAMERA"),
            intents = listOf("android.intent.action.MAIN")
        )
        
        val vector = featureExtractor.extractFeatures(appPackage).featureVector
        
        assertEquals(1f, vector[0], 0.0f)
        assertEquals(1f, vector[1], 0.0f)
        assertEquals(1f, vector[2], 0.0f)
        assertEquals(0f, vector[3], 0.0f)
    }
}
