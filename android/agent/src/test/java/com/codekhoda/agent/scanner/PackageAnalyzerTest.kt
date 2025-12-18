package com.codekhoda.agent.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PackageAnalyzer.
 * Note: Full integration tests with actual package manager should be done
 * as instrumented tests on a real device/emulator.
 */
class PackageAnalyzerTest {

    @Test
    fun `PackageAnalyzer class exists and is a valid class`() {
        // Basic test to verify the class compiles correctly
        val clazz = PackageAnalyzer::class.java
        assertNotNull(clazz)
        assertEquals("PackageAnalyzer", clazz.simpleName)
    }
    
    @Test
    fun `PackageAnalyzer has required public methods`() {
        // Verify the public API exists
        val methods = PackageAnalyzer::class.java.declaredMethods.map { it.name }
        assertTrue("getInstalledApps method should exist", methods.any { it.contains("getInstalledApps") })
        assertTrue("analyzePackage method should exist", methods.any { it.contains("analyzePackage") })
    }
}
