package com.example.scamshield.util

import com.example.scamshield.util.logD

/**
 * Thread-safe gate that prevents duplicate and spam analysis requests across
 * both the NotificationListenerService and AccessibilityService.
 *
 * Two independent checks must both pass before a request is allowed through:
 *
 *   1. Cooldown  — At least [COOLDOWN_MS] must have elapsed since the last
 *                  accepted request from this package (any content). Prevents
 *                  a burst of rapid events from flooding the backend.
 *
 *   2. Dedup     — If the text is identical to the last accepted text from this
 *                  package AND it was seen within [DEDUP_WINDOW_MS], the request
 *                  is suppressed. Stops unchanged screen content from being
 *                  re-analyzed when layout refreshes repeat the same text.
 *
 * Both services call [shouldAllow] from their respective [Dispatchers.IO]
 * coroutine contexts; the method is @Synchronized to prevent a race where two
 * threads both read a null record and both proceed simultaneously.
 *
 * Usage:
 *   if (!AnalysisCooldown.shouldAllow(packageName, text)) return
 *   // safe to launch backend coroutine here
 */
object AnalysisCooldown {

    private const val TAG = "AnalysisCooldown"

    /**
     * Minimum gap between any two accepted requests from the same package.
     * Applies regardless of whether the text content changed.
     */
    const val COOLDOWN_MS = 3_000L

    /**
     * Window during which identical text from the same package is suppressed.
     * Must be > [COOLDOWN_MS] to be meaningful; the dedup check only runs once
     * the cooldown has already cleared.
     */
    const val DEDUP_WINDOW_MS = 30_000L

    /** Stores the text and timestamp of the last accepted request per package. */
    private data class Record(val text: String, val timestamp: Long)

    // Plain HashMap is safe here because all access is guarded by @Synchronized.
    private val records = HashMap<String, Record>()

    /**
     * Returns `true` if the analysis request for [text] from [packageName] should
     * be dispatched to the backend, `false` if it should be suppressed.
     *
     * When returning `false`, the suppression reason is logged at DEBUG level so
     * the decision is visible in Logcat without being noisy at higher levels.
     *
     * Call this *before* launching any coroutine to avoid scheduling work that
     * will ultimately be discarded.
     */
    @Synchronized
    fun shouldAllow(packageName: String, text: String): Boolean {
        val now    = System.currentTimeMillis()
        val record = records[packageName]

        if (record != null) {
            val elapsed = now - record.timestamp

            // ── Cooldown check ────────────────────────────────────────────────
            // Block if the previous accepted request was too recent.
            if (elapsed < COOLDOWN_MS) {
                logD(
                    TAG,
                    "Throttled | pkg=$packageName | " +
                        "${elapsed}ms elapsed, cooldown is ${COOLDOWN_MS}ms",
                )
                return false
            }

            // ── Dedup check ───────────────────────────────────────────────────
            // Block if the text is identical and still within the dedup window.
            // (This check only runs after the cooldown has already cleared.)
            if (record.text == text && elapsed < DEDUP_WINDOW_MS) {
                logD(
                    TAG,
                    "Duplicate suppressed | pkg=$packageName | " +
                        "identical content within ${DEDUP_WINDOW_MS / 1_000}s window",
                )
                return false
            }
        }

        // Both checks passed — record this request and allow it through.
        records[packageName] = Record(text, now)
        return true
    }
}
