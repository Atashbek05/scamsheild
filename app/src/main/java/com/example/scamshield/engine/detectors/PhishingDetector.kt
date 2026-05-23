package com.example.scamshield.engine.detectors

/**
 * Detects phishing language and impersonation cues independent of links.
 *
 * Patterns deliberately overlap with [com.example.scamshield.engine.LinkScanner]
 * — links provide structural signals, this detector provides linguistic ones.
 */
object PhishingDetector {

    private val PATTERNS = listOf(
        "verify your account"     to "Account verification phish",
        "verify your identity"    to "Identity verification phish",
        "verify your credentials" to "Credential verification phish",
        "confirm your account"    to "Account confirmation phish",
        "confirm your password"   to "Password confirmation phish",
        "re-enter your password"  to "Re-auth credential harvest",
        "enter your password"     to "Credential harvest",
        "reset your password"     to "Password reset bait",
        "click the link below"    to "Link bait",
        "follow this link"        to "Link bait",
        "tap the link"            to "Link bait",
        "sign in to continue"     to "Forced sign-in",
        "session expired"         to "Forced sign-in",
        "your account has been"   to "Account-status pretext",
        "we detected suspicious"  to "Fake security alert",
        "security alert"          to "Fake security alert",
        "irs"                     to "Government impersonation",
        "tax refund"              to "Tax-refund phish",
        "shipment held"           to "Delivery phish",
        "package delivery"        to "Delivery phish",
        "customs fee"             to "Delivery phish",
        "usps"                    to "Delivery impersonation",
        "fedex"                   to "Delivery impersonation",
        "dhl"                     to "Delivery impersonation",
    )

    /** Returns matched human-readable labels (deduplicated). */
    fun detect(lowerText: String): List<String> {
        val seen = LinkedHashSet<String>()
        for ((needle, label) in PATTERNS) {
            if (lowerText.contains(needle)) seen.add(label)
        }
        return seen.toList()
    }
}
