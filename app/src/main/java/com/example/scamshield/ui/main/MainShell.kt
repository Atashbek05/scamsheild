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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.scamshield.R
import com.example.scamshield.ui.nav.NavTarget
import com.example.scamshield.ui.dashboard.DashboardScreen
import com.example.scamshield.ui.chat.AiChatScreen
import com.example.scamshield.ui.nav.MainTab
import com.example.scamshield.ui.nav.Routes
import com.example.scamshield.ui.settings.SettingsScreen
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.util.AppUpdateState

@Composable
fun MainShell(
    isNotificationGranted: Boolean,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRefreshPermissions: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenBlockedNumbers: () -> Unit = {},
    onOpenTrustedContacts: () -> Unit = {},
    onCheckForUpdates: (() -> Unit)? = null,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val pendingInner by NavTarget.inner.collectAsStateWithLifecycle()
    LaunchedEffect(pendingInner) {
        val target = pendingInner ?: return@LaunchedEffect
        navController.navigate(target) {
            popUpTo(Routes.Dashboard) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
        NavTarget.inner.value = null
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val flexibleSnackbar by AppUpdateState.showFlexibleSnackbar.collectAsStateWithLifecycle()
    val updateMsg    = stringResource(R.string.update_flexible_available)
    val updateAction = stringResource(R.string.update_action_install)

    LaunchedEffect(flexibleSnackbar) {
        if (!flexibleSnackbar) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message     = updateMsg,
            actionLabel = updateAction,
            duration    = SnackbarDuration.Long,
        )
        AppUpdateState.showFlexibleSnackbar.value = false
        if (result == SnackbarResult.ActionPerformed) {
            AppUpdateState.onStartFlexibleInstall?.invoke()
        }
    }

    Scaffold(
        containerColor = CyberBgDeep,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = CyberBgCard,
                    contentColor   = CyberTextPrimary,
                    actionColor    = CyberCyan,
                )
            }
        },
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
            onOpenPrivacyPolicy         = onOpenPrivacyPolicy,
            onOpenAbout                 = onOpenAbout,
            onOpenBlockedNumbers        = onOpenBlockedNumbers,
            onOpenTrustedContacts       = onOpenTrustedContacts,
            onCheckForUpdates           = onCheckForUpdates,
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
    onOpenPrivacyPolicy: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenBlockedNumbers: () -> Unit,
    onOpenTrustedContacts: () -> Unit,
    onCheckForUpdates: (() -> Unit)?,
) {
    NavHost(
        navController       = navController,
        startDestination    = Routes.Dashboard,
        modifier            = Modifier.padding(padding),
        enterTransition     = { fadeIn(tween(200)) },
        exitTransition      = { fadeOut(tween(200)) },
        popEnterTransition  = { fadeIn(tween(200)) },
        popExitTransition   = { fadeOut(tween(200)) },
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
        composable(Routes.AiChat)   { AiChatScreen() }
        composable(Routes.Settings) {
            SettingsScreen(
                onOpenPrivacyPolicy   = onOpenPrivacyPolicy,
                onOpenAbout           = onOpenAbout,
                onOpenBlockedNumbers  = onOpenBlockedNumbers,
                onOpenTrustedContacts = onOpenTrustedContacts,
                onCheckForUpdates     = onCheckForUpdates,
            )
        }
    }
}
