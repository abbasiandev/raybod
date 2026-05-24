package dev.abbasian.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.abbasian.data.local.entity.CachedRiskEntity

@Dao
interface RiskDao {
    @Query("SELECT * FROM risk_cache WHERE packageName = :packageName")
    suspend fun getRisk(packageName: String): CachedRiskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRisk(risk: CachedRiskEntity)

    @Query("SELECT * FROM risk_cache")
    suspend fun getAllRisks(): List<CachedRiskEntity>
}
