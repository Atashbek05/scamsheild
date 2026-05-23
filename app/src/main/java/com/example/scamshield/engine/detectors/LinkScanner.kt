package com.example.scamshield.engine.detectors

/**
 * Pulls URLs out of arbitrary text and flags them against three independent
 * suspicion classes:
 *
 *   - **Shortener** : known URL-shortening hosts that hide the real destination
 *   - **Tld**       : low-reputation top-level domains seen heavily in scams
 *   - **Pattern**   : keywords inside the URL itself (verify-account, secure-login, …)
 *   - **Impersonation** : look-alike domains for banks / brands
 *
 * Each finding is a single string in the form "host — reason" so callers can
 * surface them as chips without further parsing.
 */
object LinkScanner {

    private val LINK_REGEX = Regex("""https?://[^\s)>\]]+""", RegexOption.IGNORE_CASE)

    private val SHORT_URL_HOSTS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "rb.gy", "buff.ly",
        "is.gd", "short.link", "cutt.ly", "lnkd.in", "trib.al", "shorturl.at",
        "bl.ink", "tiny.cc", "rebrand.ly", "soo.gd", "v.gd", "tr.im", "qr.ae",
    )

    private val SUSPICIOUS_TLDS = setOf(
        ".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".click", ".country", ".rest",
        ".biz", ".info", ".top", ".loan", ".work", ".support", ".click", ".zip",
        ".mov", ".cam", ".live", ".buzz", ".online",
    )

    private val URL_KEYWORDS = listOf(
        "verify-account", "secure-login", "free-gift", "winner", "claim-now",
        "promo-code", "account-alert", "confirm-identity", "update-payment",
        "reset-password", "unlock-account", "delivery-pending", "package-update",
        "tax-refund", "irs-refund", "kyc-update", "limited-offer", "exclusive-deal",
        "win-now", "redeem-now", "click-here",
    )

    private val IMPERSONATED_BRANDS = mapOf(
        "amazon"   to listOf("amazon"),
        "apple"    to listOf("apple", "icloud"),
        "google"   to listOf("google", "gmail"),
        "microsoft" to listOf("microsoft", "office365", "outlook"),
        "paypal"   to listOf("paypal"),
        "netflix"  to listOf("netflix"),
        "facebook" to listOf("facebook", "instagram"),
        "whatsapp" to listOf("whatsapp"),
        "bank"     to listOf("bank-"),
    )

    private val LEGIT_HOSTS = setOf(
        "amazon.com", "apple.com", "google.com", "gmail.com", "microsoft.com",
        "outlook.com", "live.com", "paypal.com", "netflix.com", "facebook.com",
        "instagram.com", "whatsapp.com",
    )

    /** Parsed view of a single link finding. */
    data class LinkFinding(
        val url: String,
        val host: String,
        val reasons: List<String>,
    ) {
        val display: String get() =
            if (reasons.isEmpty()) host else "$host — ${reasons.joinToString(", ")}"
    }

    /** Result bundle returned to ExplainabilityEngine. */
    data class Report(
        val totalLinks: Int,
        val findings: List<LinkFinding>,
    )

    fun scan(text: String): Report {
        val urls = LINK_REGEX.findAll(text).map { it.value }.toList()
        val findings = mutableListOf<LinkFinding>()

        for (url in urls) {
            val host = extractHost(url)
            val reasons = mutableListOf<String>()

            if (SHORT_URL_HOSTS.any { host == it || host.endsWith(".$it") }) {
                reasons += "Shortener"
            }
            if (SUSPICIOUS_TLDS.any { host.endsWith(it) }) {
                reasons += "Suspicious TLD"
            }
            for (kw in URL_KEYWORDS) {
                if (url.contains(kw, ignoreCase = true)) {
                    reasons += "Phish keyword: $kw"
                    break
                }
            }
            for ((brand, fragments) in IMPERSONATED_BRANDS) {
                val mentions = fragments.any { host.contains(it, ignoreCase = true) }
                val legit    = LEGIT_HOSTS.any { host == it || host.endsWith(".$it") }
                if (mentions && !legit) {
                    reasons += "Impersonates $brand"
                    break
                }
            }
            // Excessively long subdomain chains are typical of phishing
            if (host.count { it == '.' } >= 4) {
                reasons += "Long subdomain chain"
            }
            // IP literal instead of a hostname
            if (host.matches(Regex("""\d+\.\d+\.\d+\.\d+"""))) {
                reasons += "Raw IP host"
            }

            if (reasons.isNotEmpty()) {
                findings += LinkFinding(url, host, reasons)
            }
        }

        // Surface a "many links" finding even if individuals look benign — legitimate
        // SMS messages rarely contain >2 URLs.
        if (findings.isEmpty() && urls.size > 2) {
            for (url in urls.take(3)) {
                findings += LinkFinding(url, extractHost(url), listOf("Link cluster"))
            }
        }
        return Report(totalLinks = urls.size, findings = findings)
    }

    private fun extractHost(url: String): String = try {
        url.removePrefix("http://").removePrefix("https://")
            .substringBefore('/').substringBefore('?').lowercase()
    } catch (_: Exception) { url }
}
