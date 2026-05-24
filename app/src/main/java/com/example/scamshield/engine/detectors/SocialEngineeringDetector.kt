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
        // Russian patterns
        "вы были выбраны"            to "False exclusivity",
        "вы выиграли"                to "False prize claim",
        "поздравляем"                to "False reward",
        "поздравляю"                 to "False reward",
        "бесплатный подарок"         to "Free-gift lure",
        "получите приз"              to "Prize lure",
        "заберите приз"              to "Prize lure",
        "ваш приз"                   to "Prize lure",
        "ваш выигрыш"                to "Prize lure",
        "подарочная карта"           to "Gift-card scam",
        "перевести деньги"           to "Direct money request",
        "отправить деньги"           to "Direct money request",
        "биткоин"                    to "Crypto payment request",
        "криптовалюта"               to "Crypto payment request",
        "крипто кошелек"             to "Crypto payment request",
        "ваш родственник"            to "Family emergency scam",
        "ваш близкий"                to "Family emergency scam",
        "попал в беду"               to "Family emergency scam",
        "попал в аварию"             to "Family emergency scam",
        "я ваш руководитель"         to "Authority impersonation",
        "это ваш директор"           to "Authority impersonation",
        "техническая поддержка"      to "Tech-support scam",
        "служба поддержки"           to "Tech-support scam",
        "инвестиционная возможность" to "Investment scam",
        "гарантированный доход"      to "Investment scam",
        "удвоить деньги"             to "Investment scam",
        "пассивный доход"            to "Investment scam",
    )

    fun detect(lowerText: String): List<String> {
        val labels = LinkedHashSet<String>()
        for ((needle, label) in PATTERNS) {
            if (lowerText.contains(needle)) labels.add(label)
        }
        return labels.toList()
    }
}
