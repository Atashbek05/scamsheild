package com.example.scamshield.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Helpers for the SYSTEM_ALERT_WINDOW ("draw over other apps") permission.
 *
 * Unlike normal runtime permissions, SYSTEM_ALERT_WINDOW is granted through a
 * dedicated system settings page — there is no `requestPermissions()` API for it.
 * The user must enable it manually via the settings intent below.
 */
object OverlayPermissionHelper {

    /**
     * Returns true if the app currently holds the SYSTEM_ALERT_WINDOW permission.
     * Must be re-checked after the user returns from the system settings screen.
     */
    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    /**
     * Opens the system "Display over other apps" settings page for this app.
     * The intent requires `FLAG_ACTIVITY_NEW_TASK` because it is launched from
     * a non-activity context (service or application).
     */
    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
