package com.example.scamshield.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.scamshield.ui.analytics.AnalyticsScreen
import com.example.scamshield.ui.call.CallProtectionScreen
import com.example.scamshield.ui.dashboard.DashboardScreen
import com.example.scamshield.ui.history.ThreatHistoryScreen
import com.example.scamshield.ui.nav.MainTab
import com.example.scamshield.ui.nav.Routes
import com.example.scamshield.ui.settings.SettingsScreen
import com.example.scamshield.ui.simulator.SimulatorScreen
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberBorder
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberTextSecondary

/**
 * Bottom-bar shell that hosts the five primary tabs. Owns its own NavController
 * because tab switches are local — the outer NavHost only sees the "main" route.
 */
@Composable
fun MainShell(
    isNotificationGranted: Boolean,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRefreshPermissions: () -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = CyberBgSurface,
                contentColor = CyberCyan,
                tonalElevation = 4.dp,
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(Routes.Dashboard) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = {
                            Text(
                                stringResource(tab.labelRes),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            indicatorColor = CyberBorder,
                            unselectedIconColor = CyberTextSecondary,
                            unselectedTextColor = CyberTextSecondary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        MainShellNav(
            padding,
            navController = navController,
            isNotificationGranted = isNotificationGranted,
            isAccessibilityGranted = isAccessibilityGranted,
            isOverlayGranted = isOverlayGranted,
            onOpenNotificationAccess = onOpenNotificationAccess,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onOpenOverlaySettings = onOpenOverlaySettings,
            onRefreshPermissions = onRefreshPermissions,
        )
    }
}

@Composable
private fun MainShellNav(
    padding: PaddingValues,
    navController: androidx.navigation.NavHostController,
    isNotificationGranted: Boolean,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRefreshPermissions: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Dashboard,
        modifier = Modifier.padding(padding),
    ) {
        composable(Routes.Dashboard) {
            DashboardScreen(
                isNotificationGranted = isNotificationGranted,
                isAccessibilityGranted = isAccessibilityGranted,
                isOverlayGranted = isOverlayGranted,
                onGrantNotificationClick = onOpenNotificationAccess,
                onGrantAccessibilityClick = onOpenAccessibilitySettings,
                onGrantOverlayClick = onOpenOverlaySettings,
                onRefreshPermissions = onRefreshPermissions,
            )
        }
        composable(Routes.Calls)     { CallProtectionScreen() }
        composable(Routes.History)   { ThreatHistoryScreen() }
        composable(Routes.Analytics) { AnalyticsScreen() }
        composable(Routes.Simulator) { SimulatorScreen() }
        composable(Routes.Settings)  { SettingsScreen() }
    }
}
