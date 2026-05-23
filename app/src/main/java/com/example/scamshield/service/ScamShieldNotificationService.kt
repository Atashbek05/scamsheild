package com.example.scamshield.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.DetectedThreat
import com.example.scamshield.data.MonitoringEvent
import com.example.scamshield.data.MonitoringStore
import com.example.scamshield.data.MonitorSource
import com.example.scamshield.data.ScanStatus
import com.example.scamshield.data.ThreatStore
import com.example.scamshield.engine.ExplainabilityEngine
import com.example.scamshield.overlay.OverlayManager
import com.example.scamshield.overlay.toOverlayData
import com.example.scamshield.repository.ScamAnalysisRepository
import com.example.scamshield.repository.ThreatRepository
import com.example.scamshield.util.AnalysisCooldown
import com.example.scamshield.util.AppNameResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Captures incoming notifications from watched messaging apps and forwards their
 * text to the ScamShield FastAPI backend for real-time scam analysis.
 *
 * Activated by the user in:
 *   Settings → Apps → Special app access → Notification access
 *
 * Request pipeline:
 *   notification posted
 *     → filter by package name
 *     → extract title + body
 *     → AnalysisCooldown gate  (suppresses duplicates and rapid bursts)
 *     → POST /analyze {"message": "<title> <body>"}
 *     → log result
 *     → update ThreatStore (dashboard state)
 *
 * Overlay warnings are shown when: probability > 70% OR backend label == "scam".
 * See [OverlayManager] for suppression rules (cooldown, dedup, single-instance).
 */
class ScamShieldNotificationService : NotificationListenerService() {

    // ── Package filter ────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "ScamShieldNLS"

        /**
         * Only notifications from these packages are forwarded for analysis.
         * Multiple variants cover regional builds and business editions.
         */
        private val WATCHED_PACKAGES = setOf(
            // Telegram (standard + FOSS/web variant)
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            // WhatsApp (consumer + Business)
            "com.whatsapp",
            "com.whatsapp.w4b",
            // SMS — Google Messages
            "com.google.android.apps.messaging",
            // SMS — Samsung Messages
            "com.samsung.android.messaging",
            // SMS — AOSP stock apps
            "com.android.mms",
            "com.android.messaging",
        )
    }

    // ── Coroutine scope ───────────────────────────────────────────────────────

    // SupervisorJob ensures a failure in one analysis coroutine does not cancel
    // siblings launched for other packages in the same notification burst.
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val repository = ScamAnalysisRepository()
    private val threatRepo: ThreatRepository by lazy { ScamShieldApp.container().threatRepository }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Listener connected — watching ${WATCHED_PACKAGES.size} packages")
        MonitoringStore.setServiceActive(MonitorSource.NOTIFICATION_LISTENER, true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Listener disconnected")
        MonitoringStore.setServiceActive(MonitorSource.NOTIFICATION_LISTENER, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        MonitoringStore.clearLiveEvent()
        MonitoringStore.setServiceActive(MonitorSource.NOTIFICATION_LISTENER, false)
        OverlayManager.dismiss()
    }

    // ── Notification capture ──────────────────────────────────────────────────

    /**
     * Called by the OS for every notification posted to the status bar.
     *
     * Extracts [Notification.EXTRA_TITLE] and [Notification.EXTRA_TEXT], skips
     * empty notifications, then runs the combined string through the cooldown
     * gate before forwarding it to the backend.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val packageName = sbn.packageName
        if (packageName !in WATCHED_PACKAGES) return

        val extras = sbn.notification?.extras ?: return

        // EXTRA_TITLE carries the sender name; EXTRA_TEXT carries the message body.
        // Joining them gives the model important context (e.g. "John: Click here").
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()?.trim().orEmpty()
        val body  = extras.getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()?.trim().orEmpty()

        if (title.isEmpty() && body.isEmpty()) return

        val message = listOf(title, body)
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        Log.d(TAG, "Captured | pkg=$packageName | \"$message\"")

        // ── Cooldown / dedup gate ─────────────────────────────────────────────
        // AnalysisCooldown enforces a minimum gap between requests from the same
        // package and suppresses identical text within a 30-second window.
        // Returns false and logs the suppression reason if the request should be skipped.
        if (!AnalysisCooldown.shouldAllow(packageName, message)) return

        analyzeWithBackend(message, packageName)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit

    // ── Backend analysis ──────────────────────────────────────────────────────

    /**
     * Dispatches [message] to the FastAPI backend on [Dispatchers.IO].
     *
     * On success:
     *   - Marks the AI engine as reachable in [ThreatStore] (shown on dashboard).
     *   - Increments the total scanned-message counter.
     *   - If scam probability ≥ 40%, records a [DetectedThreat] in [ThreatStore]
     *     so the Recent Threats feed on the dashboard updates in real-time.
     *
     * On failure:
     *   - Marks the AI engine as offline in [ThreatStore].
     *   - Detailed error is already logged by [ScamAnalysisRepository].
     */
    private fun analyzeWithBackend(message: String, packageName: String) {
        val scanId  = System.nanoTime()
        val preview = message.take(80).replace('\n', ' ')
        val quickLabel = packageName.substringAfterLast('.')

        // Publish SCANNING immediately so the UI updates before the coroutine starts.
        MonitoringStore.pushLiveEvent(
            MonitoringEvent(
                id          = scanId,
                packageName = packageName,
                appLabel    = quickLabel,
                textPreview = preview,
                status      = ScanStatus.SCANNING,
                source      = MonitorSource.NOTIFICATION_LISTENER,
            ),
        )
        Log.d(TAG, "Monitor: SCANNING | pkg=$packageName | \"${preview.take(40)}\"")

        serviceScope.launch {
            // Update to ANALYZING while the HTTP call is in-flight.
            MonitoringStore.pushLiveEvent(
                MonitoringEvent(
                    id          = scanId,
                    packageName = packageName,
                    appLabel    = quickLabel,
                    textPreview = preview,
                    status      = ScanStatus.ANALYZING,
                    source      = MonitorSource.NOTIFICATION_LISTENER,
                ),
            )
            Log.d(TAG, "Monitor: ANALYZING | pkg=$packageName")

            repository.analyzeText(
                message     = message,
                packageName = packageName,
            ).fold(
                onSuccess = { response ->
                    ThreatStore.setAiConnected(true)
                    ThreatStore.incrementScanned()

                    Log.d(
                        TAG,
                        "Result | pkg=$packageName | " +
                            "prob=${"%.3f".format(response.scamProbability)} | " +
                            "label=${response.label} | " +
                            "keywords=${response.suspiciousKeywords}",
                    )

                    val sourceAppName = AppNameResolver.resolve(applicationContext, packageName)
                    val overlayData   = response.toOverlayData(
                        capturedText  = message,
                        packageName   = packageName,
                        sourceAppName = sourceAppName,
                    )

                    // Commit result to the live monitor before any early exit.
                    val resultStatus = if (overlayData != null) ScanStatus.THREAT_DETECTED else ScanStatus.SAFE_MESSAGE
                    MonitoringStore.commitScan(
                        MonitoringEvent(
                            id          = scanId,
                            packageName = packageName,
                            appLabel    = sourceAppName,
                            textPreview = preview,
                            status      = resultStatus,
                            probability = response.scamProbability,
                            source      = MonitorSource.NOTIFICATION_LISTENER,
                        ),
                    )
                    Log.d(TAG, "Monitor: $resultStatus | pkg=$packageName | prob=${"%.3f".format(response.scamProbability)}")

                    if (overlayData == null) return@fold

                    // Run the full detection pipeline so the persisted record has
                    // category + risk-level + signals available to history/analytics.
                    val analysis = ExplainabilityEngine.analyze(
                        message            = message,
                        senderRaw          = "",
                        backendKeywords    = response.suspiciousKeywords,
                        backendProbability = overlayData.probability,
                    )
                    val threat = DetectedThreat(
                        sourcePackage  = packageName,
                        messagePreview = overlayData.messagePreview,
                        probability    = analysis.finalProbability,
                        keywords       = overlayData.keywords,
                        explanation    = analysis.explanation,
                        appLabel       = sourceAppName,
                        category       = analysis.category,
                        backendLabel   = response.label,
                    )
                    ThreatStore.addThreat(threat)
                    runCatching {
                        threatRepo.insert(threat, sourceAppName, analysis.category, response.label)
                    }.onFailure { Log.w(TAG, "Persist failed: ${it.message}") }

                    Log.d(
                        TAG,
                        "Threat recorded | pkg=$packageName | app=\"$sourceAppName\" | " +
                            "level=${overlayData.threatLevel} | category=${analysis.category} | " +
                            "prob=${"%.3f".format(analysis.finalProbability)}",
                    )

                    OverlayManager.showWarning(applicationContext, overlayData)
                },
                onFailure = {
                    ThreatStore.setAiConnected(false)
                    MonitoringStore.commitScan(
                        MonitoringEvent(
                            id          = scanId,
                            packageName = packageName,
                            appLabel    = quickLabel,
                            textPreview = preview,
                            status      = ScanStatus.ERROR,
                            source      = MonitorSource.NOTIFICATION_LISTENER,
                        ),
                    )
                    Log.d(TAG, "Monitor: ERROR | pkg=$packageName")
                    Log.w(TAG, "Analysis failed for $packageName — AI marked offline")
                },
            )
        }
    }
}
