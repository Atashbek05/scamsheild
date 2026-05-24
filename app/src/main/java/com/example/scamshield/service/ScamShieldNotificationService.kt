package com.example.scamshield.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.scamshield.util.logD
import com.example.scamshield.data.MonitoringStore
import com.example.scamshield.data.MonitorSource
import com.example.scamshield.overlay.OverlayManager
import com.example.scamshield.util.AppNameResolver

class ScamShieldNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "ScamShieldNLS"

        private val WATCHED_PACKAGES = setOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.android.messaging",
        )
    }

    private val base = object : BaseMessageAnalysisService() {
        override val monitorSource = MonitorSource.NOTIFICATION_LISTENER
        override val appContext    get() = applicationContext
        override val tag           = TAG
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        logD(TAG, "Listener connected — watching ${WATCHED_PACKAGES.size} packages")
        MonitoringStore.setServiceActive(MonitorSource.NOTIFICATION_LISTENER, true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        logD(TAG, "Listener disconnected")
        MonitoringStore.setServiceActive(MonitorSource.NOTIFICATION_LISTENER, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        base.destroy()
        MonitoringStore.clearLiveEvent()
        MonitoringStore.setServiceActive(MonitorSource.NOTIFICATION_LISTENER, false)
        OverlayManager.dismiss()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val packageName = sbn.packageName
        if (packageName !in WATCHED_PACKAGES) return

        val extras = sbn.notification?.extras ?: return

        // EXTRA_TITLE carries the sender name; EXTRA_TEXT carries the message body.
        // Joining them gives the model important context (e.g. "John: Click here").
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val body  = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()

        if (title.isEmpty() && body.isEmpty()) return

        val message = listOf(title, body).filter { it.isNotEmpty() }.joinToString(" ")

        logD(TAG, "Captured | pkg=$packageName | \"$message\"")

        base.analyzeAndReport(message, packageName, AppNameResolver.resolve(applicationContext, packageName))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit
}
