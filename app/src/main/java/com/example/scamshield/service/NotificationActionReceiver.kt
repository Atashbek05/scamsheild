package com.example.scamshield.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PAUSE_SCAN -> {
                ScanPauseManager.pauseForOneHour()
                MonitoringForegroundService.updateNotification(context)
            }
            ACTION_RESUME_SCAN -> {
                ScanPauseManager.resume()
                MonitoringForegroundService.updateNotification(context)
            }
        }
    }

    companion object {
        const val ACTION_PAUSE_SCAN  = "com.example.scamshield.ACTION_PAUSE_SCAN"
        const val ACTION_RESUME_SCAN = "com.example.scamshield.ACTION_RESUME_SCAN"
    }
}
