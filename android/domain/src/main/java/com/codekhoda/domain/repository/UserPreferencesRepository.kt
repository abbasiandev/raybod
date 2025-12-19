package com.codekhoda.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val onboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    
    val analyticsEnabled: Flow<Boolean>
    suspend fun setAnalyticsEnabled(enabled: Boolean)

    val userPlan: Flow<String>
    suspend fun setUserPlan(plan: String)

    val lastScanTimestamp: Flow<Long>
    suspend fun setLastScanTimestamp(timestamp: Long)

    val dailyScanCount: Flow<Int>
    suspend fun setDailyScanCount(count: Int)
}
