package dev.abbasian.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import dev.abbasian.domain.model.NetworkAlert
import dev.abbasian.domain.model.NetworkFlow
import dev.abbasian.domain.repository.NetworkRepository
import dev.abbasian.domain.repository.UserPreferencesRepository
import dev.abbasian.presentation.network.NetworkViewModel
import dev.abbasian.presentation.onboarding.OnboardingViewModel
import dev.abbasian.presentation.permissions.PermissionViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `NetworkViewModel - exposes flows and alerts from repository`() = runTest {
        val repository: NetworkRepository = mockk()
        val flows = listOf(mockk<NetworkFlow>())
        val alerts = listOf(mockk<NetworkAlert>())
        
        every { repository.getActiveFlows() } returns flowOf(flows)
        every { repository.getNetworkAlerts() } returns flowOf(alerts)
        
        val viewModel = NetworkViewModel(repository)
        
        // Use backgroundScope to collect flows so stateIn starts working
        val flowsJob = launch { viewModel.activeFlows.collect {} }
        val alertsJob = launch { viewModel.networkAlerts.collect {} }
        
        advanceUntilIdle()
        
        assertEquals(flows, viewModel.activeFlows.value)
        assertEquals(alerts, viewModel.networkAlerts.value)
        
        flowsJob.cancel()
        alertsJob.cancel()
    }

    @Test
    fun `OnboardingViewModel - checks status and updates repo`() = runTest {
        val repository: UserPreferencesRepository = mockk()
        every { repository.onboardingCompleted } returns flowOf(false)
        coEvery { repository.setOnboardingCompleted(true) } returns Unit
        
        val viewModel = OnboardingViewModel(repository)
        advanceUntilIdle()
        
        assertEquals(false, viewModel.uiState.value.isOnboardingCompleted)
        
        viewModel.completeOnboarding()
        advanceUntilIdle()
        
        assertEquals(true, viewModel.uiState.value.isOnboardingCompleted)
        coVerify { repository.setOnboardingCompleted(true) }
    }

    @Test
    fun `PermissionViewModel - checks system permissions`() = runTest {
        val context: Context = mockk()
        
        // Mock checkSelfPermission for QUERY_ALL_PACKAGES and POST_NOTIFICATIONS
        every { context.checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES) } returns PackageManager.PERMISSION_GRANTED
        every { context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) } returns PackageManager.PERMISSION_DENIED
        
        val viewModel = PermissionViewModel(context)
        
        val state = viewModel.uiState.value
        // Depending on SDK_INT of the test environment (Robolectric or local JVM)
        // the list might vary. We check the always-on one at least.
        assertTrue(state.permissions.any { it.name == "Always-on Protection" && it.isGranted })
        
        // If we want to test specifically the logic for R and TIRAMISU, we might need Robolectric
        // or mock the Build.VERSION.SDK_INT which is harder.
    }
}

