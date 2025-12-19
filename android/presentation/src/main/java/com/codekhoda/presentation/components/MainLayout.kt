package com.codekhoda.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.codekhoda.presentation.theme.DeepBlack
import com.codekhoda.presentation.theme.NeonCyan
import com.codekhoda.presentation.theme.NeonPink
import com.codekhoda.presentation.theme.TextPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    title: String,
    onNavigateToScan: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onExit: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {

                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("Scan") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToScan()
                    },
                    icon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = NeonCyan.copy(alpha = 0.1f)
                    )
                )
                NavigationDrawerItem(
                    label = { Text("Network Shield") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToNetwork()
                    },
                    icon = { Icon(Icons.Default.Menu, contentDescription = null, tint = NeonCyan) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )
                NavigationDrawerItem(
                    label = { Text("Security Dashboard") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSecurity()
                    },
                    icon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) }, // Should use a shield icon
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )
                NavigationDrawerItem(
                    label = { Text("About Us") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToAbout()
                    },
                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )
                Divider(Modifier.padding(vertical = 8.dp), color = NeonCyan.copy(alpha = 0.2f))
                NavigationDrawerItem(
                    label = { Text("Upgrade to Premium") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToPremium()
                    },
                    icon = { Icon(Icons.Default.Star, contentDescription = null, tint = NeonPink) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedTextColor = NeonPink,
                        unselectedIconColor = NeonPink
                    )
                )
                Divider(Modifier.padding(vertical = 8.dp), color = NeonCyan.copy(alpha = 0.2f))
                NavigationDrawerItem(
                    label = { Text("Exit") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onExit()
                    },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = NeonCyan) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonCyan
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = NeonCyan
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DeepBlack
                    )
                )
            },
            containerColor = DeepBlack,
            content = content
        )
    }
}
