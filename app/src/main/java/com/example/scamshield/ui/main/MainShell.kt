package com.example.scamshield.ui.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.scamshield.R
import com.example.scamshield.ui.chat.AiChatScreen
import com.example.scamshield.ui.components.AppNavigation
import com.example.scamshield.ui.components.NavTabConfig
import com.example.scamshield.ui.dashboard.DashboardScreen
import com.example.scamshield.ui.nav.MainTab
import com.example.scamshield.ui.nav.NavTarget
import com.example.scamshield.ui.nav.Routes
import com.example.scamshield.ui.settings.SettingsScreen
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberCyan
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
    val flexibleSnackbar  by AppUpdateState.showFlexibleSnackbar.collectAsStateWithLifecycle()
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

    val tabs = remember {
        MainTab.entries.map { NavTabConfig(it.route, it.icon, it.labelRes) }
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
            AppNavigation(
                tabs         = tabs,
                currentRoute = currentRoute,
                onTabSelected = { route ->
                    navController.navigate(route) {
                        popUpTo(Routes.Dashboard) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
            )
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
        enterTransition     = { fadeIn(tween(220)) },
        exitTransition      = { fadeOut(tween(220)) },
        popEnterTransition  = { fadeIn(tween(220)) },
        popExitTransition   = { fadeOut(tween(220)) },
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
        composable(Routes.Chat) { AiChatScreen() }
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
