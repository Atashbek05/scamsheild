package com.example.scamshield.simulator

import androidx.annotation.StringRes
import com.example.scamshield.R

/**
 * Curated scam samples used by the in-app simulator.
 *
 * Each sample is a real-world-style fake message — they never trigger network
 * calls on their own; the simulator screen feeds them to the existing analysis
 * pipeline so users can verify the engine end-to-end without an actual scammer.
 */
object ScamSamples {

    data class Sample(
        val id: String,
        val type: Type,
        val title: String,
        val sender: String,
        val message: String,
    )

    enum class Type(val label: String, val description: String, @StringRes val labelRes: Int) {
        PHISHING("Phishing",       "Credential / account-verification phish", R.string.sim_type_phishing),
        OTP("OTP Scam",            "Code exfiltration attempt",               R.string.sim_type_otp),
        FAKE_SUPPORT("Fake Support","Tech-support impersonation",             R.string.sim_type_fake_support),
        FAKE_BANKING("Fake Banking","Bank-impersonation scam",                R.string.sim_type_fake_banking),
        DELIVERY("Delivery",       "Fake parcel / customs message",           R.string.sim_type_delivery),
        LOTTERY("Lottery",         "False prize claim",                       R.string.sim_type_lottery),
        CRYPTO("Crypto",           "Investment / wallet scam",                R.string.sim_type_crypto),
        ROMANCE("Romance",         "Long-tail trust-building scam",           R.string.sim_type_romance),
        URGENT("Urgent Alert",     "Pure urgency-driven bait",                R.string.sim_type_urgent),
    }

    val all: List<Sample> = listOf(
        Sample("phish_1", Type.PHISHING, "Account verification",
            "no-reply@security",
            "We detected suspicious activity on your account. Please verify your identity immediately at https://secure-login-amazon.click/verify-account or your account will be suspended."),
        Sample("phish_2", Type.PHISHING, "Password reset bait",
            "support@accounts",
            "Your Google session has expired. Re-enter your password using this secure link to continue: http://accounts-g00gle.xyz/reset-password"),
        Sample("otp_1", Type.OTP, "OTP exfiltration",
            "+1 415 555 0144",
            "Hi, this is your bank. We sent you a verification code. Please share your code with the agent to confirm your identity."),
        Sample("otp_2", Type.OTP, "Code share request",
            "Account Helper",
            "Your one-time password is 384927. Do not share with anyone else. Reply with the code to continue."),
        Sample("support_1", Type.FAKE_SUPPORT, "Fake Microsoft support",
            "Microsoft Support",
            "URGENT: We noticed unauthorized access on your device. Call Microsoft support immediately at +1-800-555-0199 or your data will be wiped in 24 hours."),
        Sample("bank_1", Type.FAKE_BANKING, "Frozen account",
            "Chase Alerts",
            "Your Chase account has been frozen due to unusual activity. Verify your bank account at https://chase-alerts.tk/verify or complete KYC to restore access."),
        Sample("bank_2", Type.FAKE_BANKING, "SWIFT transfer",
            "branch.manager",
            "Hello, this is the branch manager. Please initiate a wire transfer of \$4,500 to the following IBAN. It is urgent — do not delay."),
        Sample("delivery_1", Type.DELIVERY, "Customs fee",
            "USPS Delivery",
            "Your package is held at customs. Pay the customs fee at https://usps-deliv.tk/package-update to release your shipment."),
        Sample("lottery_1", Type.LOTTERY, "Prize lure",
            "WINNER NOTICE",
            "Congratulations! You've been selected to receive a \$10,000 Amazon gift card. Claim your prize now at http://win-now.xyz/claim-now before the offer expires today."),
        Sample("crypto_1", Type.CRYPTO, "Investment opportunity",
            "Crypto Advisor",
            "Limited-time investment opportunity: double your money in 24 hours with our guaranteed crypto wallet. Send 0.1 BTC to the address below to get started."),
        Sample("romance_1", Type.ROMANCE, "Long-tail trust scam",
            "Anna ✨",
            "I really trust you. I need your help urgently — can you wire some money via Western Union? I'll pay you back next week. Don't tell anyone, please."),
        Sample("urgent_1", Type.URGENT, "Final warning",
            "TAX DEPARTMENT",
            "FINAL NOTICE: Your tax refund is pending. Respond immediately to claim your refund or you will lose access. Click here: http://tax-refund.gq/refund"),
    )

    fun byType(type: Type): List<Sample> = all.filter { it.type == type }
}
