package com.example.scamshield.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
 * Monitors on-screen text from watched messaging and browser apps, then forwards
 * extracted text to the ScamShield FastAPI backend for scam analysis.
 *
 * Activated by the user in:
 *   Settings → Accessibility → Downloaded apps → ScamShield
 *
 * Event types handled:
 *   TYPE_WINDOW_STATE_CHANGED    — user navigated to a new screen
 *   TYPE_WINDOW_CONTENT_CHANGED  — visible content updated (e.g. new message arrived)
 *   TYPE_NOTIFICATION_STATE_CHANGED — toast or heads-up appeared
 *
 * Capture pipeline per event:
 *   event received
 *     → filter by package name
 *     → traverse view tree (or read event text list)
 *     → truncate to [MAX_TEXT_LEN] characters
 *     → AnalysisCooldown gate  (suppresses duplicates and rapid bursts)
 *     → POST /analyze {"message": "<extracted text>"}
 *     → log result
 *     → update ThreatStore (dashboard state)
 *
 * Overlay warnings are shown when: probability > 70% OR backend label == "scam".
 * See [OverlayManager] for suppression rules (cooldown, dedup, single-instance).
 */
class ScamShieldAccessibilityService : AccessibilityService() {

    // ── Configuration ─────────────────────────────────────────────────────────

    companion object {
        const val TAG = "ScamShieldA11y"

        // Traversal depth cap — prevents stack overflows on deeply nested layouts
        // such as a RecyclerView containing many visible message bubbles.
        private const val MAX_NODE_DEPTH = 12

        // Extracted text is capped before logging and sending to avoid oversized
        // requests and excessively long log lines.
        private const val MAX_TEXT_LEN = 600

        /**
         * Apps whose on-screen content is extracted and forwarded for analysis.
         * Multiple variants cover regional builds and business editions.
         */
        val MONITORED_PACKAGES = setOf(
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
            // Chrome (stable + beta + dev)
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
        )
    }

    // ── Coroutine scope ───────────────────────────────────────────────────────

    // SupervisorJob isolates failures so one slow or failing analysis job does
    // not cancel sibling coroutines launched for other packages.
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val repository = ScamAnalysisRepository()
    private val threatRepo: ThreatRepository by lazy { ScamShieldApp.container().threatRepository }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        Log.d(TAG, "Connected — monitoring ${MONITORED_PACKAGES.size} packages")
        MonitoringStore.setServiceActive(MonitorSource.ACCESSIBILITY_SERVICE, true)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        MonitoringStore.clearLiveEvent()
        MonitoringStore.setServiceActive(MonitorSource.ACCESSIBILITY_SERVICE, false)
        OverlayManager.dismiss()
    }

    // ── Event dispatch ────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return

        // The XML config already filters by packageNames; this guard is a defensive
        // fallback in case the service config is ever updated dynamically at runtime.
        if (pkg !in MONITORED_PACKAGES) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ->
                handleWindowEvent(event, pkg)

            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ->
                handleNotificationEvent(event, pkg)
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    /**
     * Handles window-level events (screen navigation or in-place content update).
     *
     * Prefers full view-tree traversal via [AccessibilityEvent.source]; falls back
     * to [AccessibilityEvent.getText] when the source node is unavailable, which
     * is common for TYPE_WINDOW_CONTENT_CHANGED originating outside the focused window.
     */
    private fun handleWindowEvent(event: AccessibilityEvent, pkg: String) {
        val rootNode = event.source

        if (rootNode != null) {
            try {
                val texts = mutableListOf<String>()
                collectTexts(rootNode, texts, depth = 0)
                val combined = texts.joinToString(" | ").take(MAX_TEXT_LEN).trim()
                dispatchIfAllowed(pkg, eventLabel = "WINDOW", text = combined)
            } finally {
                // recycle() is a no-op on API 33+ but required on older APIs to
                // release the native AccessibilityNodeInfo reference.
                @Suppress("DEPRECATION")
                rootNode.recycle()
            }
        } else {
            val text = event.text.joinToString(" ").trim().take(MAX_TEXT_LEN)
            dispatchIfAllowed(pkg, eventLabel = "WINDOW", text = text)
        }
    }

    /**
     * Handles toast / heads-up notification events from monitored apps.
     * The full text is available directly on the event — no tree traversal needed.
     */
    private fun handleNotificationEvent(event: AccessibilityEvent, pkg: String) {
        val text = event.text.joinToString(" ").trim().take(MAX_TEXT_LEN)
        dispatchIfAllowed(pkg, eventLabel = "NOTIFICATION", text = text)
    }

    // ── View-tree traversal ───────────────────────────────────────────────────

    /**
     * Recursively collects non-empty visible text from [node] and its descendants.
     *
     * Priority: [AccessibilityNodeInfo.getText] → [AccessibilityNodeInfo.getContentDescription]
     * The content description is used as fallback for image buttons and icon-only nodes
     * (e.g. the send button in a messaging app, which is labelled "Send" but has no text).
     */
    private fun collectTexts(
        node: AccessibilityNodeInfo,
        texts: MutableList<String>,
        depth: Int,
    ) {
        if (depth > MAX_NODE_DEPTH) return

        val text        = node.text?.toString()?.trim()
        val contentDesc = node.contentDescription?.toString()?.trim()

        when {
            !text.isNullOrEmpty()        -> texts.add(text)
            !contentDesc.isNullOrEmpty() -> texts.add(contentDesc)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                collectTexts(child, texts, depth + 1)
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    // ── Cooldown gate and backend dispatch ────────────────────────────────────

    /**
     * Checks the shared [AnalysisCooldown] gate before forwarding [text] to the
     * backend. Blank text is discarded immediately without touching the gate.
     *
     * The gate enforces two rules (see [AnalysisCooldown] for full details):
     *   - Cooldown: minimum [AnalysisCooldown.COOLDOWN_MS] between requests per package.
     *   - Dedup: suppress identical text seen within [AnalysisCooldown.DEDUP_WINDOW_MS].
     *
     * When the gate allows the request, the event type and text length are logged
     * so the Logcat stream shows exactly what triggered each backend call.
     */
    private fun dispatchIfAllowed(pkg: String, eventLabel: String, text: String) {
        if (text.isBlank()) return

        // Gate check — AnalysisCooldown logs the suppression reason when returning false.
        if (!AnalysisCooldown.shouldAllow(pkg, text)) return

        Log.d(
            TAG,
            "[$eventLabel] Dispatching | pkg=$pkg | ${text.length} chars | " +
                "\"${text.take(80).replace('\n', ' ')}${if (text.length > 80) "…" else ""}\"",
        )

        analyzeWithBackend(text, pkg)
    }

    /**
     * Launches an asynchronous POST /analyze request on [Dispatchers.IO].
     *
     * On success:
     *   - Marks the AI engine as reachable in [ThreatStore] (shown on dashboard).
     *   - Increments the total scanned-message counter.
     *   - If scam probability ≥ 40%, records a [DetectedThreat] in [ThreatStore]
     *     so the Recent Threats feed on the dashboard updates in real-time.
     *
     * On failure:
     *   - Marks the AI engine as offline in [ThreatStore].
     *   - Root cause is already logged by [ScamAnalysisRepository].
     */
    private fun analyzeWithBackend(text: String, packageName: String) {
        val scanId  = System.nanoTime()
        val preview = text.take(80).replace('\n', ' ')
        val quickLabel = packageName.substringAfterLast('.')

        MonitoringStore.pushLiveEvent(
            MonitoringEvent(
                id          = scanId,
                packageName = packageName,
                appLabel    = quickLabel,
                textPreview = preview,
                status      = ScanStatus.SCANNING,
                source      = MonitorSource.ACCESSIBILITY_SERVICE,
            ),
        )
        Log.d(TAG, "Monitor: SCANNING | pkg=$packageName | \"${preview.take(40)}\"")

        serviceScope.launch {
            MonitoringStore.pushLiveEvent(
                MonitoringEvent(
                    id          = scanId,
                    packageName = packageName,
                    appLabel    = quickLabel,
                    textPreview = preview,
                    status      = ScanStatus.ANALYZING,
                    source      = MonitorSource.ACCESSIBILITY_SERVICE,
                ),
            )
            Log.d(TAG, "Monitor: ANALYZING | pkg=$packageName")

            repository.analyzeText(
                message     = text,
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
                        capturedText  = text,
                        packageName   = packageName,
                        sourceAppName = sourceAppName,
                    )

                    val resultStatus = if (overlayData != null) ScanStatus.THREAT_DETECTED else ScanStatus.SAFE_MESSAGE
                    MonitoringStore.commitScan(
                        MonitoringEvent(
                            id          = scanId,
                            packageName = packageName,
                            appLabel    = sourceAppName,
                            textPreview = preview,
                            status      = resultStatus,
                            probability = response.scamProbability,
                            source      = MonitorSource.ACCESSIBILITY_SERVICE,
                        ),
                    )
                    Log.d(TAG, "Monitor: $resultStatus | pkg=$packageName | prob=${"%.3f".format(response.scamProbability)}")

                    if (overlayData == null) return@fold

                    val analysis = ExplainabilityEngine.analyze(
                        message            = text,
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
                            source      = MonitorSource.ACCESSIBILITY_SERVICE,
                        ),
                    )
                    Log.d(TAG, "Monitor: ERROR | pkg=$packageName")
                    Log.w(TAG, "Analysis failed for $packageName — AI marked offline")
                },
            )
        }
    }
}
