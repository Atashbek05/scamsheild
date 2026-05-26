package com.example.scamshield.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.scamshield.ui.about.AboutScreen
import com.example.scamshield.ui.contacts.BlockedNumbersScreen
import com.example.scamshield.ui.contacts.TrustedContactsScreen
import com.example.scamshield.ui.main.MainShell
import com.example.scamshield.ui.onboarding.OnboardingScreen
import com.example.scamshield.ui.onboarding.PermissionsWizardScreen
import com.example.scamshield.ui.onboarding.ReadyScreen
import com.example.scamshield.ui.privacy.PrivacyPolicyScreen
import com.example.scamshield.ui.settings.SettingsViewModel
import com.example.scamshield.ui.splash.SplashScreen

/**
 * Top-level navigation: Splash → Onboarding (first run) → Permissions Wizard → MainShell.
 *
 * The start destination is decided by [UserSettings.onboardingComplete] so returning
 * users go straight to the dashboard.
 */
@Composable
fun ScamShieldNavHost(
    navController: NavHostController = rememberNavController(),
    onOpenNotificationAccess: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    isNotificationGranted: Boolean,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    onRefreshPermissions: () -> Unit,
    onCheckForUpdates: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings by settingsVm.settings.collectAsStateWithLifecycle()

    val pendingOuter by NavTarget.outer.collectAsStateWithLifecycle()
    LaunchedEffect(pendingOuter) {
        val target = pendingOuter ?: return@LaunchedEffect
        navController.navigate(target) { launchSingleTop = true }
        NavTarget.outer.value = null
    }

    NavHost(
        navController    = navController,
        startDestination = Routes.Splash,
        modifier         = modifier,
        enterTransition  = { fadeIn(tween(220)) + slideInHorizontally { it / 5 } },
        exitTransition   = { fadeOut(tween(180)) + slideOutHorizontally { -it / 5 } },
        popEnterTransition  = { fadeIn(tween(220)) + slideInHorizontally { -it / 5 } },
        popExitTransition   = { fadeOut(tween(180)) + slideOutHorizontally { it / 5 } },
    ) {
        composable(
            Routes.Splash,
            enterTransition = { EnterTransition.None },
            exitTransition  = { fadeOut(tween(350)) },
        ) {
            SplashScreen(
                onFinished = {
                    val next = if (settings.onboardingComplete) Routes.Main else Routes.Onboarding
                    navController.navigate(next) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Routes.Permissions) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                },
                onOpenPrivacyPolicy = { navController.navigate(Routes.PrivacyPolicy) },
            )
        }
        composable(Routes.PrivacyPolicy) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.About) {
            AboutScreen(
                onBack              = { navController.popBackStack() },
                onOpenPrivacyPolicy = { navController.navigate(Routes.PrivacyPolicy) },
            )
        }
        composable(Routes.Permissions) {
            PermissionsWizardScreen(
                isNotificationGranted  = isNotificationGranted,
                isAccessibilityGranted = isAccessibilityGranted,
                isOverlayGranted       = isOverlayGranted,
                onGrantNotification    = onOpenNotificationAccess,
                onGrantAccessibility   = onOpenAccessibilitySettings,
                onGrantOverlay         = onOpenOverlaySettings,
                onRefresh              = onRefreshPermissions,
                onContinue             = {
                    navController.navigate(Routes.Ready)
                },
            )
        }
        composable(Routes.Ready) {
            ReadyScreen(
                isNotificationGranted  = isNotificationGranted,
                isAccessibilityGranted = isAccessibilityGranted,
                isOverlayGranted       = isOverlayGranted,
                onGrantNotification    = onOpenNotificationAccess,
                onGrantAccessibility   = onOpenAccessibilitySettings,
                onGrantOverlay         = onOpenOverlaySettings,
                onRefresh              = onRefreshPermissions,
                onStart                = {
                    settingsVm.completeOnboarding()
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Permissions) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Main) {
            MainShell(
                isNotificationGranted   = isNotificationGranted,
                isAccessibilityGranted  = isAccessibilityGranted,
                isOverlayGranted        = isOverlayGranted,
                onOpenNotificationAccess    = onOpenNotificationAccess,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenOverlaySettings       = onOpenOverlaySettings,
                onRefreshPermissions        = onRefreshPermissions,
                onOpenPrivacyPolicy         = { navController.navigate(Routes.PrivacyPolicy) },
                onOpenAbout                 = { navController.navigate(Routes.About) },
                onOpenBlockedNumbers        = { navController.navigate(Routes.BlockedNumbers) },
                onOpenTrustedContacts       = { navController.navigate(Routes.TrustedContacts) },
                onCheckForUpdates           = onCheckForUpdates,
            )
        }
        composable(Routes.BlockedNumbers) {
            BlockedNumbersScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TrustedContacts) {
            TrustedContactsScreen(onBack = { navController.popBackStack() })
        }
    }
}
