package com.example.scamshield.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

/**
 * Helpers for checking and requesting the Accessibility Service permission.
 *
 * The OS exposes enabled accessibility services as a colon-separated list in
 * Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES. We parse that list to find
 * our own service component rather than trusting an indirect signal.
 */
object AccessibilityPermissionHelper {

    /**
     * Returns true if [ScamShieldAccessibilityService] is currently enabled in
     * the system accessibility settings.
     *
     * Component format expected in the setting:
     *   com.example.scamshield/com.example.scamshield.service.ScamShieldAccessibilityService
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val target = "${context.packageName}/" +
            "${context.packageName}.service.ScamShieldAccessibilityService"

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        // The value is a ':'-separated list of "package/component" entries.
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(target, ignoreCase = true)) return true
        }
        return false
    }

    /** Opens the system Accessibility Settings screen. */
    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
