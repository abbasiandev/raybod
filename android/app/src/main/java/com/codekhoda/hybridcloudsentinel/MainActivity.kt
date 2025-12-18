package com.codekhoda.hybridcloudsentinel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.codekhoda.presentation.scan.ScanScreen
import com.codekhoda.presentation.theme.HybridCloudSentinelTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HybridCloudSentinelTheme {
                ScanScreen()
            }
        }
    }
}
