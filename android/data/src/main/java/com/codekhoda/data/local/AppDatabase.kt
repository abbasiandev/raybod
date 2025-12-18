package com.codekhoda.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.codekhoda.data.local.dao.RiskDao
import com.codekhoda.data.local.entity.CachedRiskEntity

@Database(entities = [CachedRiskEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun riskDao(): RiskDao
}
