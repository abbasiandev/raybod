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

    private val _userPlan = MutableStateFlow(sharedPrefs.getString(Keys.USER_PLAN, "FREEMIUM") ?: "FREEMIUM")
    override val userPlan: Flow<String> = _userPlan.asStateFlow()

    private val _lastScanTimestamp = MutableStateFlow(sharedPrefs.getLong(Keys.LAST_SCAN_TIMESTAMP, 0L))
    override val lastScanTimestamp: Flow<Long> = _lastScanTimestamp.asStateFlow()

    private val _dailyScanCount = MutableStateFlow(sharedPrefs.getInt(Keys.DAILY_SCAN_COUNT, 0))
    override val dailyScanCount: Flow<Int> = _dailyScanCount.asStateFlow()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        sharedPrefs.edit().putBoolean(Keys.ONBOARDING_COMPLETED, completed).apply()
        _onboardingCompleted.value = completed
    }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(Keys.ANALYTICS_ENABLED, enabled).apply()
        _analyticsEnabled.value = enabled
    }

    override suspend fun setUserPlan(plan: String) {
        sharedPrefs.edit().putString(Keys.USER_PLAN, plan).apply()
        _userPlan.value = plan
    }

    override suspend fun setLastScanTimestamp(timestamp: Long) {
        sharedPrefs.edit().putLong(Keys.LAST_SCAN_TIMESTAMP, timestamp).apply()
        _lastScanTimestamp.value = timestamp
    }

    override suspend fun setDailyScanCount(count: Int) {
        sharedPrefs.edit().putInt(Keys.DAILY_SCAN_COUNT, count).apply()
        _dailyScanCount.value = count
    }

    private object Keys {
        const val ONBOARDING_COMPLETED = "onboarding_completed"
        const val ANALYTICS_ENABLED = "analytics_enabled"
        const val USER_PLAN = "user_plan"
        const val LAST_SCAN_TIMESTAMP = "last_scan_timestamp"
        const val DAILY_SCAN_COUNT = "daily_scan_count"
    }
}
