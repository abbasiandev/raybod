package dev.abbasian.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.abbasian.data.local.dao.NetworkDao
import dev.abbasian.data.local.dao.RiskDao
import dev.abbasian.data.local.entity.BlocklistEntity
import dev.abbasian.data.local.entity.CachedRiskEntity
import dev.abbasian.data.local.entity.NetworkAlertEntity

@Database(
    entities = [
        CachedRiskEntity::class,
        NetworkAlertEntity::class,
        BlocklistEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun riskDao(): RiskDao
    abstract fun networkDao(): NetworkDao
}
