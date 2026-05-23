package com.example.scamshield.engine.detectors

/**
 * Lightweight heuristic that scores the sender (notification title or chat name)
 * for impersonation cues without a server lookup.
 *
 * Reputation tiers:
 *   - TRUSTED   : looks like a known org / contact (still no guarantee)
 *   - UNKNOWN   : neither suspicious nor recognisable
 *   - SUSPICIOUS: emoji-heavy, ALL CAPS, spoofed brand
 */
object SenderReputation {

    enum class Tier { TRUSTED, UNKNOWN, SUSPICIOUS }

    data class Result(val tier: Tier, val reasons: List<String>)

    private val SPOOFED_BRANDS = listOf(
        "amaz0n", "g00gle", "paypa1", "micros0ft", "app1e", "netfl1x", "faceb00k",
        "support@", "no-reply@", "security@", "alert@",
    )

    private val TRUSTED_PREFIXES = listOf("verified", "support", "official")

    fun score(senderRaw: String): Result {
        val sender = senderRaw.trim()
        if (sender.isBlank()) return Result(Tier.UNKNOWN, emptyList())

        val lower = sender.lowercase()
        val reasons = mutableListOf<String>()

        if (sender.length >= 6 && sender == sender.uppercase() && sender.any { it.isLetter() }) {
            reasons += "ALL-CAPS sender"
        }
        val emojiCount = sender.count { !it.isLetterOrDigit() && it.code > 1000 }
        if (emojiCount >= 3) reasons += "Emoji-heavy sender"

        for (brand in SPOOFED_BRANDS) {
            if (lower.contains(brand)) {
                reasons += "Possible spoof: '$brand'"
                break
            }
        }

        if (lower.matches(Regex("""^\+?\d[\d\s\-]{6,}$"""))) {
            reasons += "Number-only sender"
        }

        val trustedHint = TRUSTED_PREFIXES.any { lower.startsWith(it) }

        val tier = when {
            reasons.isNotEmpty() -> Tier.SUSPICIOUS
            trustedHint          -> Tier.TRUSTED
            else                 -> Tier.UNKNOWN
        }
        return Result(tier, reasons)
    }
}
