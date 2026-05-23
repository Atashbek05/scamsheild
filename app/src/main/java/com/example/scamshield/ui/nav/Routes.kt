package com.example.scamshield.ui.nav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.scamshield.R

object Routes {
    const val Splash       = "splash"
    const val Onboarding   = "onboarding"
    const val Permissions  = "permissions"
    const val Main         = "main"
    const val Dashboard    = "dashboard"
    const val Calls        = "calls"
    const val History      = "history"
    const val Analytics    = "analytics"
    const val Simulator    = "simulator"
    const val Settings     = "settings"
    const val ThreatDetail = "threat_detail"
}

enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    Dashboard(Routes.Dashboard, "Dashboard",  Icons.Filled.Dashboard,  R.string.tab_dashboard),
    Calls(Routes.Calls,         "Calls",      Icons.Filled.Call,       R.string.tab_calls),
    History(Routes.History,     "History",    Icons.Filled.History,    R.string.tab_history),
    Analytics(Routes.Analytics, "Analytics",  Icons.Filled.Analytics,  R.string.tab_analytics),
    Simulator(Routes.Simulator, "Simulator",  Icons.Filled.BugReport,  R.string.tab_simulator),
    Settings(Routes.Settings,   "Settings",   Icons.Filled.Settings,   R.string.tab_settings);
}
