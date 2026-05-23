package com.example.scamshield.overlay

import com.example.scamshield.model.ScamAnalysisResponse

/**
 * Severity tier for an analysis result.
 *
 * Trigger conditions (per spec):
 *   RED    → probability > 70%  (high-confidence scam)
 *   YELLOW → backend label == "scam" with probability ≤ 70%  (lower-confidence backend verdict)
 *   Neither → no overlay shown (caller receives null from toOverlayData())
 *
 * Safe messages (probability ≤ 70% AND label != "scam") never reach the overlay.
 */
enum class ThreatLevel {
    RED,
    YELLOW
}

/**
 * Everything the overlay card needs to render itself.
 *
 * @param probability    Raw score from the backend [0.0, 1.0].
 * @param label          Backend verdict string ("scam" / "safe").
 * @param keywords       Words/phrases that contributed to the score.
 * @param messagePreview Truncated captured text shown in the card.
 * @param packageName    Source app package, used by OverlayManager for cooldown tracking.
 * @param sourceAppName  Human-readable app label (e.g. "Telegram"), shown in the card.
 * @param threatLevel    Pre-computed tier so the view layer never re-evaluates thresholds.
 */
data class OverlayData(
    val probability: Float,
    val label: String,
    val keywords: List<String>,
    val messagePreview: String,
    val packageName: String,
    val sourceAppName: String,
    val threatLevel: ThreatLevel
)

/**
 * Converts a backend response into overlay display data.
 *
 * Trigger conditions (both OR-ed together):
 *   - probability > 0.70  → RED  (high confidence)
 *   - label == "scam"     → RED if prob > 0.70, else YELLOW
 *
 * Returns **null** for safe messages (probability ≤ 70% AND label != "scam").
 *
 * @param capturedText  The original text that was analysed (used as card preview).
 * @param packageName   Source app package name.
 * @param sourceAppName Human-readable app label shown in the overlay card.
 */
fun ScamAnalysisResponse.toOverlayData(
    capturedText: String,
    packageName: String,
    sourceAppName: String = packageName,
): OverlayData? {
    val isScamLabel = label.lowercase() == "scam"

    val level = when {
        scamProbability > 0.70f          -> ThreatLevel.RED     // High-confidence scam
        isScamLabel                      -> ThreatLevel.YELLOW  // Backend says scam, lower prob
        else                             -> return null          // Safe message — no overlay
    }

    val preview = if (capturedText.length > 120) "${capturedText.take(120)}…" else capturedText

    return OverlayData(
        probability    = scamProbability,
        label          = label,
        keywords       = suspiciousKeywords,
        messagePreview = preview,
        packageName    = packageName,
        sourceAppName  = sourceAppName,
        threatLevel    = level,
    )
}
