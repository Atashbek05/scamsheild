package com.example.scamshield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.scamshield.data.settings.DarkMode
import com.example.scamshield.network.AiConnectionMonitor
import com.example.scamshield.service.MonitoringForegroundService
import com.example.scamshield.ui.nav.ScamShieldNavHost
import com.example.scamshield.ui.settings.SettingsViewModel
import com.example.scamshield.ui.theme.ScamShieldTheme
import com.example.scamshield.util.AccessibilityPermissionHelper
import com.example.scamshield.util.NotificationPermissionHelper
import com.example.scamshield.util.OverlayPermissionHelper

class MainActivity : ComponentActivity() {

    private var isNotificationGranted  by mutableStateOf(false)
    private var isAccessibilityGranted by mutableStateOf(false)
    private var isOverlayGranted       by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Periodic backend health checks.
        AiConnectionMonitor.start(lifecycleScope)

        // Anchor the protection stack in memory.
        MonitoringForegroundService.start(this)

        setContent {
            val settingsVm: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = SettingsViewModel.Factory)
            val settings by settingsVm.settings.collectAsStateWithLifecycle()
            val themeMode = when (settings.darkMode) {
                DarkMode.SYSTEM            -> 1
                DarkMode.DARK              -> 2
                DarkMode.ALWAYS_DARK_CYBER -> 0
            }
            ScamShieldTheme(mode = themeMode) {
                ScamShieldNavHost(
                    isNotificationGranted   = isNotificationGranted,
                    isAccessibilityGranted  = isAccessibilityGranted,
                    isOverlayGranted        = isOverlayGranted,
                    onOpenNotificationAccess = {
                        NotificationPermissionHelper.openNotificationAccessSettings(this)
                    },
                    onOpenAccessibilitySettings = {
                        AccessibilityPermissionHelper.openAccessibilitySettings(this)
                    },
                    onOpenOverlaySettings = {
                        OverlayPermissionHelper.openOverlaySettings(this)
                    },
                    onRefreshPermissions = { refreshPermissions() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        isNotificationGranted  = NotificationPermissionHelper.isNotificationAccessGranted(this)
        isAccessibilityGranted = AccessibilityPermissionHelper.isAccessibilityServiceEnabled(this)
        isOverlayGranted       = OverlayPermissionHelper.canDrawOverlays(this)
    }
}
