package com.codekhoda.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val onboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    
    val analyticsEnabled: Flow<Boolean>
    suspend fun setAnalyticsEnabled(enabled: Boolean)
}
