package dev.abbasian.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.abbasian.data.local.AppDatabase
import dev.abbasian.data.local.entity.CachedRiskEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RiskDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var riskDao: RiskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        riskDao = database.riskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and get risk result`() = runBlocking {
        // Given
        val entity = CachedRiskEntity(
            packageName = "com.test.app",
            riskLevel = "HIGH",
            threatType = "Spyware",
            description = "Test description",
            timestamp = System.currentTimeMillis(),
            heuristicsUsed = listOf("Perm1", "Intent2")
        )

        // When
        riskDao.insertRisk(entity)
        val result = riskDao.getRisk("com.test.app")

        // Then
        assertEquals(entity.packageName, result?.packageName)
        assertEquals(entity.riskLevel, result?.riskLevel)
        assertEquals(entity.threatType, result?.threatType)
        assertEquals(listOf("Perm1", "Intent2"), result?.heuristicsUsed)
    }

    @Test
    fun `get non-existent risk returns null`() = runBlocking {
        // When
        val result = riskDao.getRisk("non.existent.app")

        // Then
        assertNull(result)
    }

    @Test
    fun `overwrite existing risk updates entry`() = runBlocking {
        // Given
        val entity1 = CachedRiskEntity("com.test.app", "LOW", "", "", 100L)
        val entity2 = CachedRiskEntity("com.test.app", "CRITICAL", "Virus", "Bad", 200L)

        // When
        riskDao.insertRisk(entity1)
        riskDao.insertRisk(entity2)
        val result = riskDao.getRisk("com.test.app")

        // Then
        assertEquals("CRITICAL", result?.riskLevel)
        assertEquals("Virus", result?.threatType)
    }
}
