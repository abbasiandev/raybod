package com.codekhoda.hybridcloudsentinel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.codekhoda.presentation.about.AboutScreen
import com.codekhoda.presentation.components.MainLayout
import com.codekhoda.presentation.scan.ScanScreen
import com.codekhoda.presentation.theme.HybridCloudSentinelTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.Modifier

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HybridCloudSentinelTheme {
                val navController = rememberNavController()
                
                MainLayout(
                    title = "Sentinel Dashboard",
                    onNavigateToScan = { navController.navigate("scan") {
                        popUpTo("scan") { inclusive = true }
                    } },
                    onNavigateToAbout = { navController.navigate("about") },
                    onExit = { finish() }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "scan",
                        modifier = Modifier.padding(paddingValues)
                    ) {

                        composable("scan") {
                            ScanScreen()
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
