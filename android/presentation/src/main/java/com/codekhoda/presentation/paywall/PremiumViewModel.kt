package com.codekhoda.presentation.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codekhoda.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userPlan: StateFlow<String> = userPreferencesRepository.userPlan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "FREEMIUM")

    fun updatePlan(isPaid: Boolean) {
        viewModelScope.launch {
            if (isPaid) {
                userPreferencesRepository.setUserPlan("PREMIUM")
            } else {
                userPreferencesRepository.setUserPlan("FREEMIUM")
            }
        }
    }
}



