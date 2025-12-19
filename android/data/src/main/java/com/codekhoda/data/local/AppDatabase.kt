package com.codekhoda.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.codekhoda.data.local.dao.NetworkDao
import com.codekhoda.data.local.dao.RiskDao
import com.codekhoda.data.local.entity.BlocklistEntity
import com.codekhoda.data.local.entity.CachedRiskEntity
import com.codekhoda.data.local.entity.NetworkAlertEntity

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
