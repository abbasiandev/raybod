package dev.abbasian.data.mapper

import dev.abbasian.data.local.entity.CachedRiskEntity
import dev.abbasian.domain.model.RiskAssessment
import dev.abbasian.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    @Test
    fun `CachedRiskEntity toDomain maps all fields correctly`() {
        // Given
        val entity = CachedRiskEntity(
            packageName = "com.test.app",
            riskLevel = "HIGH",
            threatType = "Spyware",
            description = "Suspicious permissions detected",
            timestamp = 1702900000000L,
            heuristicsUsed = listOf("PermissionA")
        )

        // When
        val domain = entity.toDomain()

        // Then
        assertEquals("com.test.app", domain.packageName)
        assertEquals(RiskLevel.HIGH, domain.riskLevel)
        assertEquals("Spyware", domain.threatType)
        assertEquals("Suspicious permissions detected", domain.description)
        assertEquals(1702900000000L, domain.timestamp)
        assertEquals(listOf("PermissionA"), domain.heuristicsUsed)
    }

    @Test
    fun `RiskAssessment toEntity maps all fields correctly`() {
        // Given
        val assessment = RiskAssessment(
            packageName = "com.test.app",
            riskLevel = RiskLevel.CRITICAL,
            threatType = "Malware",
            description = "Known malware signature",
            timestamp = 1702900000000L,
            heuristicsUsed = listOf("HeuristicX")
        )

        // When
        val entity = assessment.toEntity()

        // Then
        assertEquals("com.test.app", entity.packageName)
        assertEquals("CRITICAL", entity.riskLevel)
        assertEquals("Malware", entity.threatType)
        assertEquals("Known malware signature", entity.description)
        assertEquals(1702900000000L, entity.timestamp)
        assertEquals(listOf("HeuristicX"), entity.heuristicsUsed)
    }

    @Test
    fun `roundtrip conversion preserves data`() {
        // Given
        val originalAssessment = RiskAssessment(
            packageName = "com.roundtrip.test",
            riskLevel = RiskLevel.MEDIUM,
            threatType = "Adware",
            description = "Potentially unwanted program",
            timestamp = 1702900000000L,
            heuristicsUsed = listOf("H1", "H2")
        )

        // When
        val entity = originalAssessment.toEntity()
        val convertedBack = entity.toDomain()

        // Then
        assertEquals(originalAssessment.packageName, convertedBack.packageName)
        assertEquals(originalAssessment.riskLevel, convertedBack.riskLevel)
        assertEquals(originalAssessment.threatType, convertedBack.threatType)
        assertEquals(originalAssessment.description, convertedBack.description)
        assertEquals(originalAssessment.timestamp, convertedBack.timestamp)
        assertEquals(originalAssessment.heuristicsUsed, convertedBack.heuristicsUsed)
    }

    @Test
    fun `all RiskLevel values map correctly between entity and domain`() {
        RiskLevel.entries.forEach { level ->
            // Given
            val entity = CachedRiskEntity(
                packageName = "com.test.${level.name}",
                riskLevel = level.name,
                threatType = "",
                description = "Test",
                timestamp = 0L
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals(level, domain.riskLevel)
        }
    }

    @Test
    fun `invalid riskLevel string maps to UNKNOWN`() {
        // Given
        val entity = CachedRiskEntity(
            packageName = "com.test.invalid",
            riskLevel = "INVALID_LEVEL",
            threatType = "",
            description = "Test",
            timestamp = 0L
        )

        // When
        val domain = entity.toDomain()

        // Then
        assertEquals(RiskLevel.UNKNOWN, domain.riskLevel)
    }
}
