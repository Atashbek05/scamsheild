package com.example.scamshield.data.call

import androidx.annotation.StringRes
import com.example.scamshield.R

/**
 * Tiered severity for an incoming call. Mirrors the message-side [com.example.scamshield.data.RiskLevel]
 * but is dedicated to the call-protection pipeline so the two domains can evolve independently.
 *
 * Thresholds (probability is 0.0..1.0):
 *   TRUSTED  ≤ 0.10   contact in trusted list, or known safe
 *   SAFE     ≤ 0.30   no scam signals
 *   LOW      ≤ 0.50   weak signal (unknown number, no extra flags)
 *   MEDIUM   ≤ 0.70   multiple weak signals or one strong
 *   HIGH     ≤ 0.90   strong scam signal — show overlay
 *   CRITICAL > 0.90   confirmed scam pattern (matches DB or fake-bank pattern)
 */
enum class CallRisk(
    val label: String,
    val threshold: Float,
    @StringRes val labelRes: Int,
) {
    TRUSTED ("Trusted",  0.00f, R.string.call_risk_trusted),
    SAFE    ("Safe",     0.11f, R.string.call_risk_safe),
    LOW     ("Low",      0.31f, R.string.call_risk_low),
    MEDIUM  ("Medium",   0.51f, R.string.call_risk_medium),
    HIGH    ("High",     0.71f, R.string.call_risk_high),
    CRITICAL("Critical", 0.91f, R.string.call_risk_critical);

    companion object {
        fun fromProbability(p: Float): CallRisk = when {
            p >= CRITICAL.threshold -> CRITICAL
            p >= HIGH.threshold     -> HIGH
            p >= MEDIUM.threshold   -> MEDIUM
            p >= LOW.threshold      -> LOW
            p >= SAFE.threshold     -> SAFE
            else                    -> TRUSTED
        }
    }
}

/**
 * Reason flags that contributed to a call risk score.  Each flag carries its
 * weight (added to the running probability) and a human-readable explanation
 * for the "Why this call is dangerous" overlay section.
 */
enum class CallFlag(
    val weight: Float,
    @StringRes val explanationRes: Int,
) {
    UNKNOWN_NUMBER       (0.15f, R.string.call_flag_unknown_number),
    REPEATED_UNKNOWN     (0.20f, R.string.call_flag_repeated_unknown),
    INTERNATIONAL_PREFIX (0.20f, R.string.call_flag_international_prefix),
    HIGH_RISK_PREFIX     (0.30f, R.string.call_flag_high_risk_prefix),
    SHORT_NUMBER         (0.20f, R.string.call_flag_short_number),
    FAKE_BANK_PATTERN    (0.45f, R.string.call_flag_fake_bank),
    SCAM_KEYWORD         (0.35f, R.string.call_flag_scam_keyword),
    ROBOCALL_BEHAVIOR    (0.30f, R.string.call_flag_robocall),
    SHORT_DURATION_SPAM  (0.20f, R.string.call_flag_short_duration),
    REPORTED_NUMBER      (0.50f, R.string.call_flag_reported_number),
    BLOCKED_DB_MATCH     (0.95f, R.string.call_flag_blocked_db),
    NO_CALLER_ID         (0.18f, R.string.call_flag_no_caller_id),
}
