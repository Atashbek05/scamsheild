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
        // Russian patterns
        "подтвердите аккаунт"         to "Account verification phish",
        "подтвердите личность"        to "Identity verification phish",
        "подтвердите пароль"          to "Password confirmation phish",
        "введите пароль"              to "Credential harvest",
        "сменить пароль"              to "Password reset bait",
        "перейдите по ссылке"         to "Link bait",
        "нажмите на ссылку"           to "Link bait",
        "перейти по ссылке"           to "Link bait",
        "войдите в аккаунт"           to "Forced sign-in",
        "сессия истекла"              to "Forced sign-in",
        "ваш аккаунт заблокирован"    to "Account-freeze threat",
        "аккаунт заблокирован"        to "Account-freeze threat",
        "обнаружена подозрительная"   to "Fake security alert",
        "предупреждение безопасности" to "Fake security alert",
        "возврат налога"              to "Tax-refund phish",
        "налоговый вычет"             to "Tax-refund phish",
        "ваша посылка"                to "Delivery phish",
        "задержана на таможне"        to "Delivery phish",
        "доставка задержана"          to "Delivery phish",
        "почта россия"                to "Delivery impersonation",
        "госуслуги"                   to "Government impersonation",
        "фнс"                         to "Government impersonation",
        "налоговая служба"            to "Government impersonation",
        "мвд"                         to "Government impersonation",
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
