package com.example.scamshield.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Re-launches the foreground monitoring service on device boot so users don't
 * have to open the app for protection to resume.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("ScamShieldBoot", "Boot completed — starting monitoring foreground service")
            MonitoringForegroundService.start(context)
        }
    }
}
