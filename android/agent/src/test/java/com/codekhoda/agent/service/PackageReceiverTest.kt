package com.codekhoda.agent.service

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for PackageReceiver behavior.
 * Note: These are basic unit tests. Integration tests with actual Android
 * broadcast receiver behavior should be done as instrumented tests.
 */
class PackageReceiverTest {

    private val receiver = PackageReceiver()

    @Test
    fun `receiver class exists and is instantiable`() {
        // Basic smoke test to ensure the receiver can be instantiated
        val receiver = PackageReceiver()
        assertEquals(PackageReceiver::class.java, receiver.javaClass)
    }

    @Test
    fun `companion object constants are correctly defined in SentinelService`() {
        // Verify the constants that PackageReceiver uses
        assertEquals("com.codekhoda.agent.SCAN_PACKAGE", SentinelService.ACTION_SCAN_PACKAGE)
        assertEquals("package_name", SentinelService.EXTRA_PACKAGE_NAME)
    }
}
