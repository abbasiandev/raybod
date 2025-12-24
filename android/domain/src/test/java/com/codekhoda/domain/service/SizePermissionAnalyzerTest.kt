package com.codekhoda.domain.service

import com.codekhoda.domain.model.AppPackage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SizePermissionAnalyzerTest {

    @Test
    fun `analyzeAnomalies flags tiny app with dangerous permissions`() {
        val tinyApp = AppPackage(
            packageName = "com.suspicious.app",
            versionCode = 1,
            signature = "hash",
            installedSize = 200 * 1024L, // 200KB (under 2MB threshold)
            permissions = listOf(
                "android.permission.CAMERA",
                "android.permission.SEND_SMS",
                "android.permission.RECEIVE_SMS",  // Added to trigger SMS detection
                "android.permission.READ_EXTERNAL_STORAGE"
            ),
            appLabel = "Suspicious App"
        )

        val anomalies = SizePermissionAnalyzer.analyzeAnomalies(tinyApp)

        // New analyzer only flags HIGH-RISK permissions (SMS)
        assertTrue("Should detect SMS permissions", anomalies.any { it.contains("SMS") })
        // With 2+ high-risk perms, might also flag camera+storage combo
        assertTrue("Should have at least one anomaly", anomalies.isNotEmpty())
    }

    @Test
    fun `analyzeAnomalies ignores normal sized app`() {
        val normalApp = AppPackage(
            packageName = "com.normal.app",
            versionCode = 1,
            signature = "hash",
            installedSize = 10 * 1024 * 1024L, // 10MB
            permissions = listOf("android.permission.CAMERA")
        )

        val anomalies = SizePermissionAnalyzer.analyzeAnomalies(normalApp)

        assertTrue(anomalies.isEmpty())
    }

    @Test
    fun `analyzeAnomalies ignores tiny app with safe permissions`() {
        val safeTinyApp = AppPackage(
            packageName = "com.tiny.safe",
            versionCode = 1,
            signature = "hash",
            installedSize = 100 * 1024L, // 100KB
            permissions = listOf("android.permission.INTERNET")
        )

        val anomalies = SizePermissionAnalyzer.analyzeAnomalies(safeTinyApp)

        assertTrue(anomalies.isEmpty())
    }
}
