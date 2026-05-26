package com.example.scamshield.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.scamshield.MainActivity
import com.example.scamshield.R
import com.example.scamshield.data.ThreatCategory
import java.util.concurrent.atomic.AtomicInteger

object ThreatNotificationHelper {

    const val EXTRA_NAVIGATE_TO    = "navigate_to"
    const val EXTRA_BLOCK_PACKAGE  = "block_package"
    const val NAV_HISTORY          = "history"
    const val NAV_BLOCKED_NUMBERS  = "blocked_numbers"

    private val nextId = AtomicInteger(5_000)

    fun postThreatNotification(
        context: Context,
        category: ThreatCategory,
        messagePreview: String,
        sourcePackage: String,
    ) {
        MonitoringForegroundService.ensureChannels(context)

        val id = nextId.getAndIncrement()
        val categoryLabel = context.getString(category.labelRes)
        val title = context.getString(R.string.notif_threat_title, categoryLabel)
        val body  = messagePreview.take(60)

        val detailIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAVIGATE_TO, NAV_HISTORY)
        }
        val detailPi = PendingIntent.getActivity(
            context, id * 2,
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val blockIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAVIGATE_TO, NAV_BLOCKED_NUMBERS)
            putExtra(EXTRA_BLOCK_PACKAGE, sourcePackage)
        }
        val blockPi = PendingIntent.getActivity(
            context, id * 2 + 1,
            blockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, MonitoringForegroundService.CHANNEL_THREATS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0L, 400L, 200L, 400L))
            .addAction(0, context.getString(R.string.notif_threat_action_details), detailPi)
            .addAction(0, context.getString(R.string.notif_threat_action_block), blockPi)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notification)
    }
}
