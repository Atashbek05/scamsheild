package com.example.scamshield.ui.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.scamshield.ui.dashboard.DashboardScreen
import com.example.scamshield.ui.history.ThreatHistoryScreen
import com.example.scamshield.ui.nav.MainTab
import com.example.scamshield.ui.nav.Routes
import com.example.scamshield.ui.settings.SettingsScreen
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberTextMuted

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
        containerColor = CyberBgDeep,
        bottomBar = {
            NavigationBar(
                modifier       = Modifier.height(64.dp),
                containerColor = CyberBgSurface,
                tonalElevation = 0.dp,
            ) {
                MainTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick  = {
                            navController.navigate(tab.route) {
                                popUpTo(Routes.Dashboard) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = stringResource(tab.labelRes),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = {
                            Text(
                                stringResource(tab.labelRes),
                                fontSize   = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = CyberBgDeep,
                            selectedTextColor   = CyberCyan,
                            indicatorColor      = CyberCyan,
                            unselectedIconColor = CyberTextMuted,
                            unselectedTextColor = CyberTextMuted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        MainShellNav(
            padding       = padding,
            navController = navController,
            isNotificationGranted   = isNotificationGranted,
            isAccessibilityGranted  = isAccessibilityGranted,
            isOverlayGranted        = isOverlayGranted,
            onOpenNotificationAccess    = onOpenNotificationAccess,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onOpenOverlaySettings       = onOpenOverlaySettings,
            onRefreshPermissions        = onRefreshPermissions,
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
        navController    = navController,
        startDestination = Routes.Dashboard,
        modifier         = Modifier.padding(padding),
    ) {
        composable(Routes.Dashboard) {
            DashboardScreen(
                isNotificationGranted   = isNotificationGranted,
                isAccessibilityGranted  = isAccessibilityGranted,
                isOverlayGranted        = isOverlayGranted,
                onGrantNotificationClick    = onOpenNotificationAccess,
                onGrantAccessibilityClick   = onOpenAccessibilitySettings,
                onGrantOverlayClick         = onOpenOverlaySettings,
                onRefreshPermissions        = onRefreshPermissions,
            )
        }
        composable(Routes.History)  { ThreatHistoryScreen() }
        composable(Routes.Settings) { SettingsScreen() }
    }
}
