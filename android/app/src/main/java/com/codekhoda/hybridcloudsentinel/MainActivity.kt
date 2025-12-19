package com.codekhoda.hybridcloudsentinel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.codekhoda.presentation.about.AboutScreen
import com.codekhoda.presentation.components.MainLayout
import com.codekhoda.presentation.onboarding.OnboardingScreen
import com.codekhoda.presentation.onboarding.OnboardingViewModel
import com.codekhoda.presentation.permissions.PermissionDashboardScreen
import com.codekhoda.presentation.scan.ScanScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HybridCloudSentinelTheme {
                val navController = rememberNavController()
                val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                val onboardingState by onboardingViewModel.uiState.collectAsState()

                if (!onboardingState.isOnboardingCompleted) {
                    OnboardingScreen(onComplete = { onboardingViewModel.completeOnboarding() })
                } else {
                    MainLayout(
                        title = "Sentinel Dashboard",
                        onNavigateToScan = {
                            navController.navigate("scan") {
                                popUpTo("scan") { inclusive = true }
                            }
                        },
                        onNavigateToSecurity = { navController.navigate("security") },
                        onNavigateToAbout = { navController.navigate("about") },
                        onExit = { finish() }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = "scan",
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable("scan") {
                                ScanScreen(onNavigateToSecurity = { navController.navigate("security") })
                            }
                            composable("security") {
                                PermissionDashboardScreen()
                            }
                            composable("about") {
                                AboutScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
