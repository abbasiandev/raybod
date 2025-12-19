package com.codekhoda.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codekhoda.presentation.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    title: String,
    userPlan: String = "FREEMIUM",
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
    var showExitDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(NeonCyan.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SENTINEL",
                                style = MaterialTheme.typography.headlineMedium,
                                color = NeonCyan,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp
                            )
                            if (userPlan == "PREMIUM") {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = NeonPink,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (userPlan == "PREMIUM") "PREMIUM PROTECTION ACTIVE" else "v1.0.0 PROTECTED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (userPlan == "PREMIUM") NeonPink else SafeGreen
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("System Scan", fontWeight = FontWeight.Medium) },
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
                    label = { Text("Network Shield", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToNetwork()
                    },
                    icon = { Icon(Icons.Default.Share, contentDescription = null, tint = NeonCyan) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )
                NavigationDrawerItem(
                    label = { Text("Security Health", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSecurity()
                    },
                    icon = { Icon(Icons.Default.Build, contentDescription = null, tint = NeonCyan) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    )
                )
                NavigationDrawerItem(
                    label = { Text("About System", fontWeight = FontWeight.Medium) },
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
                
                if (userPlan != "PREMIUM") {
                    Divider(Modifier.padding(vertical = 16.dp, horizontal = 24.dp), color = NeonCyan.copy(alpha = 0.1f))
                    NavigationDrawerItem(
                        label = { Text("Upgrade Premium", fontWeight = FontWeight.Bold) },
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
                }
                
                Spacer(Modifier.weight(1f))
                NavigationDrawerItem(
                    label = { Text("Terminate Session") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showExitDialog = true
                    },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = AlertRed) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        unselectedTextColor = TextMuted
                    )
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
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
                    // Color Separation Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        NeonCyan,
                                        NeonPurple,
                                        NeonPink,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            },
            containerColor = DeepBlack,
            content = content
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "TERMINATE SESSION?",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to exit the application?",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onExit()
                    }
                ) {
                    Text("EXIT", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("CANCEL", color = NeonCyan)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
