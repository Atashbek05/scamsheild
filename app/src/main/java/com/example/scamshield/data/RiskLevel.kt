package com.example.scamshield.data

import androidx.annotation.StringRes
import com.example.scamshield.R

/**
 * Risk-level tiers used everywhere the UI needs to classify a threat:
 * dashboard cards, threat history filters, analytics breakdown, PDF report.
 *
 * Probability thresholds:
 *   SAFE     prob <  0.30
 *   LOW      prob >= 0.30
 *   MEDIUM   prob >= 0.50
 *   HIGH     prob >= 0.70
 *   CRITICAL prob >= 0.90
 */
enum class RiskLevel(val label: String, val threshold: Float, @StringRes val labelRes: Int) {
    SAFE("Safe",     0.0f,  R.string.risk_safe),
    LOW("Low",       0.30f, R.string.risk_low),
    MEDIUM("Medium", 0.50f, R.string.risk_medium),
    HIGH("High",     0.70f, R.string.risk_high),
    CRITICAL("Critical", 0.90f, R.string.risk_critical);

    companion object {
        fun fromProbability(p: Float): RiskLevel = when {
            p >= CRITICAL.threshold -> CRITICAL
            p >= HIGH.threshold     -> HIGH
            p >= MEDIUM.threshold   -> MEDIUM
            p >= LOW.threshold      -> LOW
            else                    -> SAFE
        }
    }
}

/**
 * High-level scam category derived from detector signals + backend keywords.
 * Used by analytics charts and the explainability cards.
 */
enum class ThreatCategory(val label: String, @StringRes val labelRes: Int) {
    PHISHING("Phishing",                     R.string.category_phishing),
    OTP_SCAM("OTP Scam",                     R.string.category_otp_scam),
    FAKE_BANKING("Fake Banking",             R.string.category_fake_banking),
    SOCIAL_ENGINEERING("Social Engineering", R.string.category_social_engineering),
    URGENCY_BAIT("Urgency Bait",             R.string.category_urgency_bait),
    LINK_TRAP("Link Trap",                   R.string.category_link_trap),
    OTHER("Other Scam",                      R.string.category_other);

    companion object {
        fun fromString(value: String): ThreatCategory =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}
