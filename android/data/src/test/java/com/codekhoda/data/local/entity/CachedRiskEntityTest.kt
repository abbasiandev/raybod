package com.codekhoda.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CachedRiskEntityTest {

    @Test
    fun `create CachedRiskEntity with all fields`() {
        val entity = CachedRiskEntity(
            packageName = "com.test.cached",
            riskLevel = "HIGH",
            threatType = "Spyware",
            description = "Test description",
            timestamp = 1702900000000L
        )

        assertEquals("com.test.cached", entity.packageName)
        assertEquals("HIGH", entity.riskLevel)
        assertEquals("Spyware", entity.threatType)
        assertEquals("Test description", entity.description)
        assertEquals(1702900000000L, entity.timestamp)
    }

    @Test
    fun `packageName is the primary key`() {
        val entity1 = CachedRiskEntity(
            packageName = "com.unique.key",
            riskLevel = "SAFE",
            threatType = "",
            description = "Test",
            timestamp = 1000L
        )
        val entity2 = entity1.copy(timestamp = 2000L)

        // Same package name means same logical entity
        assertEquals(entity1.packageName, entity2.packageName)
    }

    @Test
    fun `equality is based on all fields`() {
        val entity1 = CachedRiskEntity(
            packageName = "com.test",
            riskLevel = "SAFE",
            threatType = "",
            description = "Test",
            timestamp = 1000L
        )
        val entity2 = CachedRiskEntity(
            packageName = "com.test",
            riskLevel = "SAFE",
            threatType = "",
            description = "Test",
            timestamp = 1000L
        )
        val entity3 = entity1.copy(riskLevel = "HIGH")

        assertEquals(entity1, entity2)
        assertNotEquals(entity1, entity3)
    }

    @Test
    fun `riskLevel stores enum name as string`() {
        // Given we store RiskLevel.HIGH.name
        val entity = CachedRiskEntity(
            packageName = "com.test",
            riskLevel = "HIGH", // Stored as string
            threatType = "",
            description = "",
            timestamp = 0L
        )

        assertEquals("HIGH", entity.riskLevel)
    }

    @Test
    fun `all risk level strings are valid`() {
        val validLevels = listOf("SAFE", "LOW", "MEDIUM", "HIGH", "CRITICAL", "UNKNOWN")

        validLevels.forEach { level ->
            val entity = CachedRiskEntity(
                packageName = "com.test.$level",
                riskLevel = level,
                threatType = "",
                description = "",
                timestamp = 0L
            )
            assertEquals(level, entity.riskLevel)
        }
    }

    @Test
    fun `copy creates independent instance`() {
        val original = CachedRiskEntity(
            packageName = "com.original",
            riskLevel = "SAFE",
            threatType = "",
            description = "Original",
            timestamp = 1000L
        )

        val copy = original.copy(description = "Modified")

        assertEquals("Original", original.description)
        assertEquals("Modified", copy.description)
        assertEquals(original.packageName, copy.packageName)
    }

    @Test
    fun `empty strings are handled correctly`() {
        val entity = CachedRiskEntity(
            packageName = "",
            riskLevel = "",
            threatType = "",
            description = "",
            timestamp = 0L
        )

        assertEquals("", entity.packageName)
        assertEquals("", entity.riskLevel)
        assertEquals("", entity.threatType)
        assertEquals("", entity.description)
    }
}
