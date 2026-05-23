package com.example.scamshield.data.call

/**
 * Outcome the call-protection pipeline applied to an analysed call.
 *
 *   ALLOWED  — call ringtone went through normally (low risk)
 *   SCREENED — call was silenced/sent to system screen UI (medium risk)
 *   BLOCKED  — call was rejected outright via CallScreeningService (high risk + auto-block)
 *   REPORTED — user tapped "Report" from the overlay
 */
enum class CallAction { ALLOWED, SCREENED, BLOCKED, REPORTED }

/**
 * Decision returned by the [com.example.scamshield.engine.call.PhoneNumberAnalyzer]
 * for a single incoming call.
 */
data class CallAnalysis(
    val phoneNumber: String,
    val displayName: String,
    val probability: Float,
    val risk: CallRisk,
    val flags: List<CallFlag>,
    val explanation: String,
    val isBlocked: Boolean = false,
    val isTrusted: Boolean = false,
)

/**
 * One row in the live call feed.  Combines the analysis with the action that was
 * actually applied so the dashboard can render history without joining streams.
 */
data class CallEvent(
    val id: Long,
    val phoneNumber: String,
    val displayName: String,
    val probability: Float,
    val risk: CallRisk,
    val flags: List<CallFlag>,
    val explanation: String,
    val action: CallAction,
    val durationSeconds: Int,
    val timestamp: Long,
)
