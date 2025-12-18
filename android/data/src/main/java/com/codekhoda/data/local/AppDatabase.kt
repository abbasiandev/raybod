package com.codekhoda.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.codekhoda.data.local.dao.RiskDao
import com.codekhoda.data.local.entity.CachedRiskEntity

@Database(entities = [CachedRiskEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun riskDao(): RiskDao
}
