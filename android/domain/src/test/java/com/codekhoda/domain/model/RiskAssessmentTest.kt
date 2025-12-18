package com.codekhoda.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskAssessmentTest {

    @Test
    fun `create RiskAssessment with required fields`() {
        val assessment = RiskAssessment(
            packageName = "com.malware.test",
            riskLevel = RiskLevel.CRITICAL,
            description = "Malware detected"
        )

        assertEquals("com.malware.test", assessment.packageName)
        assertEquals(RiskLevel.CRITICAL, assessment.riskLevel)
        assertEquals("Malware detected", assessment.description)
        assertEquals("", assessment.threatType) // Default
        assertTrue(assessment.heuristicsUsed.isEmpty()) // Default
        assertNotNull(assessment.timestamp) // Should have current time
    }

    @Test
    fun `create RiskAssessment with all fields`() {
        val heuristics = listOf("PermissionCombo", "Blocklist")
        val timestamp = 1702900000000L
        
        val assessment = RiskAssessment(
            packageName = "com.spyware.example",
            riskLevel = RiskLevel.HIGH,
            threatType = "Spyware",
            description = "High risk permission combination detected",
            timestamp = timestamp,
            heuristicsUsed = heuristics
        )

        assertEquals("com.spyware.example", assessment.packageName)
        assertEquals(RiskLevel.HIGH, assessment.riskLevel)
        assertEquals("Spyware", assessment.threatType)
        assertEquals("High risk permission combination detected", assessment.description)
        assertEquals(timestamp, assessment.timestamp)
        assertEquals(2, assessment.heuristicsUsed.size)
        assertTrue(assessment.heuristicsUsed.contains("PermissionCombo"))
    }

    @Test
    fun `safe assessment has appropriate properties`() {
        val safeAssessment = RiskAssessment(
            packageName = "com.safe.app",
            riskLevel = RiskLevel.SAFE,
            threatType = "",
            description = "No threats detected"
        )

        assertEquals(RiskLevel.SAFE, safeAssessment.riskLevel)
        assertEquals("", safeAssessment.threatType)
    }

    @Test
    fun `different risk levels create correct assessments`() {
        RiskLevel.entries.forEach { level ->
            val assessment = RiskAssessment(
                packageName = "com.test.${level.name.lowercase()}",
                riskLevel = level,
                description = "Test for $level"
            )
            assertEquals(level, assessment.riskLevel)
        }
    }

    @Test
    fun `RiskAssessment equality check`() {
        val assessment1 = RiskAssessment(
            packageName = "com.test",
            riskLevel = RiskLevel.MEDIUM,
            description = "Test",
            timestamp = 1000L
        )
        val assessment2 = RiskAssessment(
            packageName = "com.test",
            riskLevel = RiskLevel.MEDIUM,
            description = "Test",
            timestamp = 1000L
        )

        assertEquals(assessment1, assessment2)
        assertEquals(assessment1.hashCode(), assessment2.hashCode())
    }

    @Test
    fun `copy creates modified assessment`() {
        val original = RiskAssessment(
            packageName = "com.original",
            riskLevel = RiskLevel.LOW,
            description = "Original assessment"
        )
        
        val upgraded = original.copy(riskLevel = RiskLevel.HIGH)

        assertEquals(RiskLevel.LOW, original.riskLevel)
        assertEquals(RiskLevel.HIGH, upgraded.riskLevel)
        assertEquals(original.packageName, upgraded.packageName)
    }
}
