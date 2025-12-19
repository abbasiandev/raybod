package com.codekhoda.presentation.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        val permissions = mutableListOf<PermissionStatus>()

        // 1. QUERY_ALL_PACKAGES (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions.add(PermissionStatus(
                name = "Package Scanning",
                description = "Required to analyze all installed apps for threats.",
                isGranted = context.checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES) == PackageManager.PERMISSION_GRANTED,
                permission = Manifest.permission.QUERY_ALL_PACKAGES,
                minSdk = 30
            ))
        }

        // 2. POST_NOTIFICATIONS (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             permissions.add(PermissionStatus(
                name = "Real-time Alerts",
                description = "Sentinel needs notification access to alert you immediately of threats.",
                isGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
                permission = Manifest.permission.POST_NOTIFICATIONS,
                minSdk = 33
            ))
        }

        // 3. Foreground Service
        permissions.add(PermissionStatus(
                name = "Always-on Protection",
                description = "Runs background analysis to keep you safe while the app is closed.",
                isGranted = true, // Simplified for now as it's declared in manifest
                permission = "",
                minSdk = 26
            ))

        _uiState.value = _uiState.value.copy(permissions = permissions)
    }
}

data class PermissionStatus(
    val name: String,
    val description: String,
    val isGranted: Boolean,
    val permission: String,
    val minSdk: Int
)

data class PermissionUiState(
    val permissions: List<PermissionStatus> = emptyList()
)
