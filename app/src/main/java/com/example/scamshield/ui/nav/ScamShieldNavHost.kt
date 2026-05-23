package com.example.scamshield.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.scamshield.ui.main.MainShell
import com.example.scamshield.ui.onboarding.OnboardingScreen
import com.example.scamshield.ui.onboarding.PermissionsWizardScreen
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
    modifier: Modifier = Modifier,
) {
    val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings by settingsVm.settings.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
        modifier = modifier,
    ) {
        composable(Routes.Splash) {
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
                onOpenNotificationAccess = onOpenNotificationAccess,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenOverlaySettings   = onOpenOverlaySettings,
                onRefreshPermissions    = onRefreshPermissions,
            )
        }
    }
}
