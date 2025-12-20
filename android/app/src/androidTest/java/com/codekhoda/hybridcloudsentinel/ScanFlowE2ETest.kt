package com.codekhoda.hybridcloudsentinel

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.codekhoda.domain.model.AppPackage
import com.codekhoda.domain.model.RiskAssessment
import com.codekhoda.domain.model.RiskLevel
import com.codekhoda.domain.repository.ThreatRepository
import com.codekhoda.domain.repository.UserPreferencesRepository
import com.codekhoda.agent.scanner.PackageAnalyzer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.BindValue
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ScanFlowE2ETest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @BindValue
    @JvmField
    val threatRepository: ThreatRepository = mockk()

    @BindValue
    @JvmField
    val packageAnalyzer: PackageAnalyzer = mockk()

    @BindValue
    @JvmField
    val userPreferencesRepository: UserPreferencesRepository = mockk()

    @Before
    fun setup() {
        // Force onboarding as completed so we start at ScanScreen
        coEvery { userPreferencesRepository.onboardingCompleted } returns flowOf(true)
        coEvery { userPreferencesRepository.userPlan } returns flowOf("FREEMIUM")
        
        hiltRule.inject()
    }

    @Test
    fun testScanFlow_initiatesAndShowsResults() {
        // Given
        val app = AppPackage("com.test.app", 1, signature = "sig")
        val assessment = RiskAssessment(
            packageName = "com.test.app", 
            riskLevel = RiskLevel.HIGH, 
            description = "Threat detected", 
            threatType = "Spyware"
        )
        
        coEvery { packageAnalyzer.getInstalledApps() } returns listOf(app)
        coEvery { threatRepository.scanApp(any()) } returns assessment

        // When
        // ScanScreen has a button with text "INITIATE SYSTEM SCAN"
        composeTestRule.onNodeWithText("INITIATE SYSTEM SCAN").performClick()

        // Then
        // Wait for scan to complete (it has some artificial delays in ViewModel)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("SCAN RESULTS").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify result is displayed
        // ResultItemCard shows result.packageName.substringAfterLast('.')
        composeTestRule.onNodeWithText("app").assertExists()
        composeTestRule.onNodeWithText("HIGH").assertExists()
    }
}




