package com.example.scamshield.engine.detectors

/**
 * Identifies OTP/verification-code scam patterns — the user is socially engineered
 * to share a code, or a fake verification request mimics a legitimate one.
 */
object OtpScamDetector {

    private val OTP_PHRASES = listOf(
        "your otp"            to "OTP request",
        "otp code"            to "OTP code request",
        "one time password"   to "OTP language",
        "one-time password"   to "OTP language",
        "verification code"   to "Verification code request",
        "share your code"     to "Share-code social engineering",
        "do not share"        to "Mimics legit OTP warning",
        "code is"             to "OTP claim",
        "code:"               to "OTP-style payload",
        "your code is"        to "OTP claim",
        "enter the code"      to "OTP entry request",
        "send me the code"    to "Code exfiltration",
        "give me the code"    to "Code exfiltration",
        "tell me the code"    to "Code exfiltration",
        "confirm with code"   to "Code confirm request",
    )

    /** True if the text contains a digit sequence that looks like an OTP (4–8 digits). */
    private val OTP_CODE_REGEX = Regex("""(?<![\w])(\d{4,8})(?![\w])""")

    fun detect(lowerText: String): List<String> {
        val labels = LinkedHashSet<String>()
        for ((needle, label) in OTP_PHRASES) {
            if (lowerText.contains(needle)) labels.add(label)
        }
        // Only flag bare-code patterns when at least one phrase already matched —
        // a plain phone-number or amount in a non-OTP message would false-positive.
        if (labels.isNotEmpty() && OTP_CODE_REGEX.containsMatchIn(lowerText)) {
            labels.add("Numeric OTP detected")
        }
        return labels.toList()
    }
}
