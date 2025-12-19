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
            installedSize = 200 * 1024L, // 200KB
            permissions = listOf(
                "android.permission.CAMERA",
                "android.permission.SEND_SMS",
                "android.permission.READ_EXTERNAL_STORAGE"
            )
        )

        val anomalies = SizePermissionAnalyzer.analyzeAnomalies(tinyApp)

        assertEquals(3, anomalies.size)
        assertTrue(anomalies.any { it.contains("requesting camera access") })
        assertTrue(anomalies.any { it.contains("requesting SMS access") })
        assertTrue(anomalies.any { it.contains("requesting storage access") })
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
