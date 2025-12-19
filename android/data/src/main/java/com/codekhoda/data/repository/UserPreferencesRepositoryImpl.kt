package com.codekhoda.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.codekhoda.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val context: Context
) : UserPreferencesRepository {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _onboardingCompleted = MutableStateFlow(sharedPrefs.getBoolean(Keys.ONBOARDING_COMPLETED, false))
    override val onboardingCompleted: Flow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _analyticsEnabled = MutableStateFlow(sharedPrefs.getBoolean(Keys.ANALYTICS_ENABLED, false))
    override val analyticsEnabled: Flow<Boolean> = _analyticsEnabled.asStateFlow()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        sharedPrefs.edit().putBoolean(Keys.ONBOARDING_COMPLETED, completed).apply()
        _onboardingCompleted.value = completed
    }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(Keys.ANALYTICS_ENABLED, enabled).apply()
        _analyticsEnabled.value = enabled
    }

    private object Keys {
        const val ONBOARDING_COMPLETED = "onboarding_completed"
        const val ANALYTICS_ENABLED = "analytics_enabled"
    }
}
