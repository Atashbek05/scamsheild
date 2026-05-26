package com.example.scamshield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.scamshield.MainActivity
import com.example.scamshield.R
import com.example.scamshield.service.ThreatNotificationHelper.EXTRA_NAVIGATE_TO
import com.example.scamshield.service.ThreatNotificationHelper.NAV_HISTORY
import com.example.scamshield.util.logD

/**
 * Long-running foreground service that anchors the ScamShield monitoring stack
 * in memory so the system is less likely to kill the notification listener or
 * accessibility service in low-memory conditions.
 */
class MonitoringForegroundService : Service() {

    companion object {
        private const val TAG          = "ScamShieldFg"
        const val CHANNEL_MONITORING   = "scamshield_monitoring"
        const val CHANNEL_THREATS      = "scamshield_threats"
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

        fun updateNotification(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, buildNotification(context))
        }

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (nm.getNotificationChannel(CHANNEL_MONITORING) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_MONITORING,
                        context.getString(R.string.notif_channel_name),
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = context.getString(R.string.notif_channel_description)
                        setShowBadge(false)
                    },
                )
            }

            if (nm.getNotificationChannel(CHANNEL_THREATS) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_THREATS,
                        context.getString(R.string.notif_threats_channel_name),
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = context.getString(R.string.notif_threats_channel_description)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0L, 400L, 200L, 400L)
                    },
                )
            }
        }

        private fun buildNotification(context: Context): Notification {
            val paused = ScanPauseManager.isPaused()

            val title = context.getString(
                if (paused) R.string.notif_protection_paused else R.string.notif_protection_active,
            )
            val text = context.getString(
                if (paused) R.string.notif_paused_text else R.string.notif_monitoring_text,
            )

            // "Threat History" action — navigates to History tab
            val historyIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAVIGATE_TO, NAV_HISTORY)
            }
            val historyPi = PendingIntent.getActivity(
                context, 1001,
                historyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            // Pause or Resume action
            val toggleAction = if (paused) {
                val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_RESUME_SCAN
                }
                val pi = PendingIntent.getBroadcast(
                    context, 1002, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                NotificationCompat.Action(0, context.getString(R.string.notif_action_resume), pi)
            } else {
                val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_PAUSE_SCAN
                }
                val pi = PendingIntent.getBroadcast(
                    context, 1003, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                NotificationCompat.Action(0, context.getString(R.string.notif_action_pause), pi)
            }

            return NotificationCompat.Builder(context, CHANNEL_MONITORING)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(0, context.getString(R.string.notif_action_history), historyPi)
                .addAction(toggleAction)
                .build()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannels(this)
        startForeground(NOTIFICATION_ID, buildNotification(this))
        ScamShieldTileService.requestUpdate(this)
        logD(TAG, "ScamShield foreground monitoring started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        ScamShieldTileService.requestUpdate(this)
        logD(TAG, "ScamShield foreground monitoring stopped")
    }
}
