package com.codekhoda.presentation.paywall

import com.codekhoda.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PremiumViewModelTest {

    private lateinit var userPrefs: UserPreferencesRepository
    private lateinit var viewModel: PremiumViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userPrefs = mockk()
        
        every { userPrefs.userPlan } returns flowOf("FREEMIUM")
        coEvery { userPrefs.setUserPlan(any()) } returns Unit
        
        viewModel = PremiumViewModel(userPrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial plan matches repository`() = runTest {
        // When
        val plan = viewModel.userPlan.first()

        // Then
        assertEquals("FREEMIUM", plan)
    }

    @Test
    fun `updatePlan to true sets PREMIUM in repository`() = runTest {
        // When
        viewModel.updatePlan(true)
        advanceUntilIdle()

        // Then
        coVerify { userPrefs.setUserPlan("PREMIUM") }
    }

    @Test
    fun `updatePlan to false sets FREEMIUM in repository`() = runTest {
        // When
        viewModel.updatePlan(false)
        advanceUntilIdle()

        // Then
        coVerify { userPrefs.setUserPlan("FREEMIUM") }
    }
}



