package dev.abbasian.data.local.dao

import androidx.room.*
import dev.abbasian.data.local.entity.BlocklistEntity
import dev.abbasian.data.local.entity.NetworkAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDao {
    @Query("SELECT * FROM network_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<NetworkAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: NetworkAlertEntity)

    @Query("SELECT pattern FROM network_blocklist")
    suspend fun getBlocklistPatterns(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocklist(entries: List<BlocklistEntity>)

    @Query("DELETE FROM network_blocklist")
    suspend fun clearBlocklist()
}




