package dev.abbasian.raybod

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import dev.abbasian.domain.repository.UserPreferencesRepository
import dev.abbasian.presentation.about.AboutScreen
import dev.abbasian.presentation.components.MainLayout
import dev.abbasian.presentation.onboarding.OnboardingScreen
import dev.abbasian.presentation.onboarding.OnboardingViewModel
import dev.abbasian.presentation.permissions.PermissionDashboardScreen
import dev.abbasian.presentation.network.NetworkDashboardScreen
import dev.abbasian.presentation.paywall.PremiumScreen
import dev.abbasian.presentation.scan.ScanScreen
import dev.abbasian.presentation.scan.ScanViewModel
import dev.abbasian.presentation.scan.ThreatDetailsScreen
import dev.abbasian.presentation.theme.RaybodTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("sentinel_prefs", Context.MODE_PRIVATE)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            RaybodTheme {
                val navController = rememberNavController()
                val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                val onboardingState by onboardingViewModel.uiState.collectAsState()
                val userPlan by userPreferencesRepository.userPlan.collectAsState("FREEMIUM")

                if (!onboardingState.isOnboardingCompleted) {
                    OnboardingScreen(onComplete = { onboardingViewModel.completeOnboarding() })
                } else {
                    MainLayout(
                        title = "Sentinel Dashboard",
                        userPlan = userPlan,
                        onNavigateToScan = {
                            navController.navigate("scan") {
                                popUpTo("scan") { inclusive = true }
                            }
                        },
                        onNavigateToNetwork = { navController.navigate("network") },
                        onNavigateToSecurity = { navController.navigate("security") },
                        onNavigateToAbout = { navController.navigate("about") },
                        onNavigateToPremium = { navController.navigate("premium") },
                        onExit = { finish() }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = "scan",
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable("scan") {
                                val scanViewModel: ScanViewModel = hiltViewModel()
                                ScanScreen(
                                    viewModel = scanViewModel,
                                    userPlan = userPlan,
                                    onNavigateToSecurity = { navController.navigate("security") },
                                    onNavigateToPremium = { navController.navigate("premium") },
                                    onNavigateToThreatDetails = { packageName ->
                                        navController.navigate("threat_details/$packageName")
                                    }
                                )
                            }
                            composable("threat_details/{packageName}") { backStackEntry ->
                                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                                val parentEntry = remember(backStackEntry) {
                                    navController.getBackStackEntry("scan")
                                }
                                val scanViewModel: ScanViewModel = hiltViewModel(parentEntry)
                                ThreatDetailsScreen(
                                    packageName = packageName,
                                    viewModel = scanViewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("network") {
                                NetworkDashboardScreen()
                            }
                            composable("security") {
                                PermissionDashboardScreen()
                            }
                            composable("about") {
                                AboutScreen()
                            }
                            composable("premium") {
                                PremiumScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "codekhoda" && uri.host == "payment-result") {
                val status = uri.getQueryParameter("status")
                if (status == "success") {
                    lifecycleScope.launch {
                        userPreferencesRepository.setUserPlan("PREMIUM")
                    }
                }
            }
        }
    }
}
