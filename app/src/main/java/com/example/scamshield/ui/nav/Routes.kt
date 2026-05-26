package com.example.scamshield.ui.nav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.scamshield.R

object Routes {
    const val Splash       = "splash"
    const val Onboarding   = "onboarding"
    const val Permissions  = "permissions"
    const val Main         = "main"
    const val Dashboard    = "dashboard"
    const val AiChat       = "ai_chat"
    const val Settings     = "settings"
    const val ThreatDetail  = "threat_detail"
    const val PrivacyPolicy    = "privacy_policy"
    const val Ready            = "ready"
    const val About            = "about"
    const val BlockedNumbers   = "blocked_numbers"
    const val TrustedContacts  = "trusted_contacts"
}

enum class MainTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    Dashboard(Routes.Dashboard, "Shield",   Icons.Rounded.Dashboard, R.string.tab_dashboard),
    AiChat   (Routes.AiChat,    "AI Chat",  Icons.Default.Chat,      R.string.tab_ai_chat),
    Settings (Routes.Settings,  "Settings", Icons.Rounded.Settings,  R.string.tab_settings),
}
