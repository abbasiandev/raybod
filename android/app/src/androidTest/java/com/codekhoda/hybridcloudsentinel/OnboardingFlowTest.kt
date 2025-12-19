package com.codekhoda.hybridcloudsentinel

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.codekhoda.domain.repository.UserPreferencesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.BindValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OnboardingFlowTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)

    private val onboardingCompletedFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        coEvery { userPreferencesRepository.onboardingCompleted } returns onboardingCompletedFlow
        coEvery { userPreferencesRepository.userPlan } returns MutableStateFlow("FREEMIUM")
        
        hiltRule.inject()
    }

    @Test
    fun testOnboarding_navigationAndCompletion() {
        // 1. Initial page
        composeTestRule.onNodeWithText("HYBRID CLOUD SENTINEL").assertExists()
        
        // 2. Click NEXT
        composeTestRule.onNodeWithText("NEXT").performClick()
        composeTestRule.onNodeWithText("LIGHTWEIGHT & EFFICIENT").assertExists()
        
        // 3. Click NEXT
        composeTestRule.onNodeWithText("NEXT").performClick()
        composeTestRule.onNodeWithText("FREE & PREMIUM").assertExists()
        
        // 4. Click NEXT
        composeTestRule.onNodeWithText("NEXT").performClick()
        composeTestRule.onNodeWithText("TRUST-FIRST SECURITY").assertExists()
        
        // 5. Click GET STARTED
        composeTestRule.onNodeWithText("GET STARTED").performClick()
        
        // Verify repository was called to save completion
        coVerify { userPreferencesRepository.setOnboardingCompleted(true) }
    }

    @Test
    fun testOnboarding_skip() {
        // 1. Initial page
        composeTestRule.onNodeWithText("SKIP").performClick()
        
        // Verify repository was called to save completion
        coVerify { userPreferencesRepository.setOnboardingCompleted(true) }
    }
}

