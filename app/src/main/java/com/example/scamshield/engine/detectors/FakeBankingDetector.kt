package com.example.scamshield.engine.detectors

/**
 * Detects fake-bank impersonation language — both the bank-name patterns and
 * the financial-action triggers ("transfer", "wire", "frozen account") that
 * typical scams pair with.
 */
object FakeBankingDetector {

    private val BANK_KEYWORDS = listOf(
        "your bank"             to "Bank-account pretext",
        "bank account"          to "Bank-account pretext",
        "account suspended"     to "Account-freeze threat",
        "account frozen"        to "Account-freeze threat",
        "account locked"        to "Account-freeze threat",
        "account blocked"       to "Account-freeze threat",
        "unusual activity"      to "Fake fraud alert",
        "suspicious login"      to "Fake fraud alert",
        "unauthorized access"   to "Fake fraud alert",
        "wire transfer"         to "Wire-transfer demand",
        "transfer funds"        to "Funds-transfer demand",
        "swift code"            to "Wire-transfer demand",
        "iban"                  to "Wire-transfer demand",
        "atm card"               to "Card-impersonation cue",
        "debit card"            to "Card-impersonation cue",
        "credit card"           to "Card-impersonation cue",
        "card has been"         to "Card alert pretext",
        "your card"             to "Card alert pretext",
        "kyc"                   to "Fake KYC request",
        "complete kyc"          to "Fake KYC request",
        "re-verify kyc"         to "Fake KYC request",
        "branch manager"        to "Fake branch impersonation",
        "ifsc"                  to "Indian-bank impersonation",
    )

    /** Common bank names — used to mark messages that name-drop a bank. */
    private val BANK_NAMES = setOf(
        "chase", "citibank", "wells fargo", "bank of america", "boa",
        "hsbc", "barclays", "lloyds", "santander", "rbc", "tdbank", "scotiabank",
        "sbi", "hdfc", "icici", "axis bank", "kotak", "pnb",
        "deutsche bank", "ubs", "ing", "bnp paribas",
        "halyk", "kaspi", "forte bank", "bcc", "qazaq banki",
    )

    fun detect(lowerText: String): List<String> {
        val labels = LinkedHashSet<String>()
        for ((needle, label) in BANK_KEYWORDS) {
            if (lowerText.contains(needle)) labels.add(label)
        }
        for (name in BANK_NAMES) {
            if (lowerText.contains(name)) {
                labels.add("Bank name dropped: ${name.replaceFirstChar { it.uppercase() }}")
                break // mention at most one to avoid spam
            }
        }
        return labels.toList()
    }
}
