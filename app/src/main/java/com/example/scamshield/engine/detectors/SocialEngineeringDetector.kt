package com.example.scamshield.engine.detectors

/**
 * Detects classical social-engineering patterns: false rewards, fear, authority abuse,
 * money/crypto demands, fake-help offers.
 */
object SocialEngineeringDetector {

    private val PATTERNS = listOf(
        "you've been selected"   to "False exclusivity",
        "you have been selected" to "False exclusivity",
        "you are selected"       to "False exclusivity",
        "congratulations"        to "False reward",
        "you have won"           to "False prize claim",
        "you've won"             to "False prize claim",
        "free gift"              to "Free-gift lure",
        "free prize"             to "Free-prize lure",
        "claim your prize"       to "Prize lure",
        "claim your reward"      to "Reward lure",
        "gift card"              to "Gift-card scam",
        "amazon gift"            to "Gift-card scam",
        "send money"             to "Direct money request",
        "send funds"             to "Direct money request",
        "wire money"             to "Wire-transfer scam",
        "western union"          to "Wire-transfer scam",
        "moneygram"              to "Wire-transfer scam",
        "bitcoin"                to "Crypto payment request",
        "btc address"            to "Crypto payment request",
        "ethereum"               to "Crypto payment request",
        "usdt"                   to "Crypto payment request",
        "crypto wallet"          to "Crypto payment request",
        "trusted source"         to "Trust manipulation",
        "your bank account"      to "Bank-account bait",
        "i need your help"       to "Help-pretext bait",
        "this is your boss"      to "Authority impersonation",
        "ceo of"                 to "Authority impersonation",
        "from your manager"      to "Authority impersonation",
        "tech support"           to "Tech-support scam",
        "microsoft support"      to "Tech-support scam",
        "apple support"          to "Tech-support scam",
        "we noticed"             to "Concern-pretext",
        "for your safety"        to "Concern-pretext",
        "investment opportunity" to "Investment scam",
        "guaranteed return"      to "Investment scam",
        "double your money"      to "Investment scam",
    )

    fun detect(lowerText: String): List<String> {
        val labels = LinkedHashSet<String>()
        for ((needle, label) in PATTERNS) {
            if (lowerText.contains(needle)) labels.add(label)
        }
        return labels.toList()
    }
}
