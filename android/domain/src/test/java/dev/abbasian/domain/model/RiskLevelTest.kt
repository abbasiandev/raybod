package dev.abbasian.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RiskLevelTest {

    @Test
    fun `all risk levels are defined`() {
        val expectedLevels = listOf("SAFE", "LOW", "MEDIUM", "HIGH", "CRITICAL", "UNKNOWN")
        val actualLevels = RiskLevel.entries.map { it.name }
        
        assertEquals(expectedLevels.size, actualLevels.size)
        expectedLevels.forEach { expected ->
            assertNotNull("Missing risk level: $expected", RiskLevel.valueOf(expected))
        }
    }

    @Test
    fun `risk levels have correct ordinal order`() {
        // SAFE should be the least severe, CRITICAL should be the most severe
        assertEquals(0, RiskLevel.SAFE.ordinal)
        assertEquals(1, RiskLevel.LOW.ordinal)
        assertEquals(2, RiskLevel.MEDIUM.ordinal)
        assertEquals(3, RiskLevel.HIGH.ordinal)
        assertEquals(4, RiskLevel.CRITICAL.ordinal)
        assertEquals(5, RiskLevel.UNKNOWN.ordinal)
    }

    @Test
    fun `string to enum conversion works correctly`() {
        assertEquals(RiskLevel.SAFE, RiskLevel.valueOf("SAFE"))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.valueOf("CRITICAL"))
        assertEquals(RiskLevel.UNKNOWN, RiskLevel.valueOf("UNKNOWN"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid string throws exception`() {
        RiskLevel.valueOf("INVALID")
    }
}
