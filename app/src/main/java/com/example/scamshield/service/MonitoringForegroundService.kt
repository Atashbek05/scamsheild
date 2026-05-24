package com.example.scamshield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.example.scamshield.util.logD
import androidx.core.app.NotificationCompat
import com.example.scamshield.R

/**
 * Long-running foreground service that anchors the ScamShield monitoring stack
 * in memory so the system is less likely to kill the notification listener or
 * accessibility service in low-memory conditions.
 *
 * The service does no work itself — it just keeps a sticky notification visible
 * so the OS treats the process as foreground priority.
 */
class MonitoringForegroundService : Service() {

    companion object {
        private const val TAG = "ScamShieldFg"
        private const val CHANNEL_ID = "scamshield_monitoring"
        private const val NOTIFICATION_ID = 4242

        fun start(context: Context) {
            val intent = Intent(context, MonitoringForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitoringForegroundService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        logD(TAG, "ScamShield foreground monitoring started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        logD(TAG, "ScamShield foreground monitoring stopped")
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.notif_channel_description)
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.notif_protection_active))
            .setContentText(getString(R.string.notif_monitoring_text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
