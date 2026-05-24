package com.example.scamshield.engine.call

import android.content.Context
import com.example.scamshield.util.logD
import com.example.scamshield.R
import com.example.scamshield.data.call.CallAnalysis
import com.example.scamshield.data.call.CallFlag
import com.example.scamshield.data.call.CallRisk
import com.example.scamshield.data.call.CallStore
import com.example.scamshield.repository.CallRepository

/**
 * Heuristic risk analyser for an incoming phone number.
 *
 * Runs entirely on-device — combines:
 *   • Blocked / trusted database lookups
 *   • Number-shape heuristics (length, country prefix)
 *   • Caller-ID keyword scan (fake bank / support / robocall vocabulary)
 *   • Repetition cache (same unknown number ringing multiple times)
 *
 * Each matched signal contributes a [CallFlag], its weight is summed into a
 * probability score, and the score is mapped to [CallRisk].  A short
 * "Why this call is dangerous" explanation is composed from the matched flags.
 */
class PhoneNumberAnalyzer(
    private val context: Context,
    private val repository: CallRepository,
) {
    /**
     * Analyse a single incoming call.  Pure heuristics — no network calls so
     * this is safe to invoke from a CallScreeningService where latency budgets
     * are tight.
     *
     * @param rawNumber     E.164-ish phone number, may be null for blocked caller ID
     * @param callerIdHint  Optional CNAM / display name received from telephony
     */
    suspend fun analyze(rawNumber: String?, callerIdHint: String? = null): CallAnalysis {
        val number = normalize(rawNumber)
        val displayName = callerIdHint?.trim().orEmpty()

        if (number.isBlank()) {
            return buildResult(
                number       = "",
                displayName  = displayName.ifBlank { context.getString(R.string.call_unknown_caller) },
                flags        = listOf(CallFlag.NO_CALLER_ID, CallFlag.UNKNOWN_NUMBER),
                isBlocked    = false,
                isTrusted    = false,
            ).also { logD(TAG, "Hidden caller ID → risk=${it.risk}") }
        }

        val isBlocked = runCatching { repository.isBlocked(number) }.getOrDefault(false)
        val isTrusted = runCatching { repository.isTrusted(number) }.getOrDefault(false)

        if (isTrusted) {
            return CallAnalysis(
                phoneNumber = number,
                displayName = displayName.ifBlank { context.getString(R.string.call_trusted_contact) },
                probability = 0.0f,
                risk        = CallRisk.TRUSTED,
                flags       = emptyList(),
                explanation = context.getString(R.string.call_explain_trusted),
                isTrusted   = true,
            ).also { logD(TAG, "Trusted contact → bypass: $number") }
        }

        val flags = mutableListOf<CallFlag>()

        if (isBlocked) flags += CallFlag.BLOCKED_DB_MATCH

        // ── Number-shape heuristics ──────────────────────────────────────────
        if (number.length in 3..5)   flags += CallFlag.SHORT_NUMBER
        if (looksInternational(number)) flags += CallFlag.INTERNATIONAL_PREFIX
        if (highRiskPrefix(number))   flags += CallFlag.HIGH_RISK_PREFIX

        // ── Display-name keyword scan ────────────────────────────────────────
        val nameLower = displayName.lowercase()
        if (matchesAnyKeyword(nameLower, FAKE_BANK_KEYWORDS)) flags += CallFlag.FAKE_BANK_PATTERN
        if (matchesAnyKeyword(nameLower, SUPPORT_KEYWORDS) ||
            matchesAnyKeyword(nameLower, SCAM_KEYWORDS)
        ) {
            flags += CallFlag.SCAM_KEYWORD
        }
        if (matchesAnyKeyword(nameLower, ROBOCALL_KEYWORDS)) flags += CallFlag.ROBOCALL_BEHAVIOR

        // ── Session repetition ───────────────────────────────────────────────
        val sessionRepeats = CallStore.repeatCountFor(number)
        val recentDbHits   = runCatching {
            repository.countByNumberSince(number, System.currentTimeMillis() - REPEAT_WINDOW_MS)
        }.getOrDefault(0)
        if (sessionRepeats + recentDbHits >= REPEAT_THRESHOLD) {
            flags += CallFlag.REPEATED_UNKNOWN
        }

        // ── Unknown-number baseline ─────────────────────────────────────────
        if (CallFlag.BLOCKED_DB_MATCH !in flags && nameLower.isBlank()) {
            flags += CallFlag.UNKNOWN_NUMBER
        }

        val resolvedName = displayName.ifBlank { context.getString(R.string.call_unknown_caller) }

        return buildResult(
            number      = number,
            displayName = resolvedName,
            flags       = flags,
            isBlocked   = isBlocked,
            isTrusted   = false,
        ).also {
            logD(
                TAG,
                "Analysed call → number=$number prob=${"%.2f".format(it.probability)} " +
                    "risk=${it.risk} flags=${it.flags.map { f -> f.name }}",
            )
        }
    }

    private fun buildResult(
        number: String,
        displayName: String,
        flags: List<CallFlag>,
        isBlocked: Boolean,
        isTrusted: Boolean,
    ): CallAnalysis {
        val rawScore = flags.sumOf { it.weight.toDouble() }.toFloat()
        val probability = rawScore.coerceIn(0f, 1f)
        val risk = CallRisk.fromProbability(probability)
        val explanation = composeExplanation(displayName, flags)
        return CallAnalysis(
            phoneNumber = number,
            displayName = displayName,
            probability = probability,
            risk        = risk,
            flags       = flags,
            explanation = explanation,
            isBlocked   = isBlocked,
            isTrusted   = isTrusted,
        )
    }

    /**
     * "Why this call is dangerous" — composes a short paragraph from the
     * matched flags so the overlay can show plain-English reasoning.
     */
    private fun composeExplanation(displayName: String, flags: List<CallFlag>): String {
        if (flags.isEmpty()) {
            return context.getString(R.string.call_explain_no_signals)
        }
        if (CallFlag.BLOCKED_DB_MATCH in flags) {
            return context.getString(R.string.call_explain_blocked)
        }
        val reasons = flags.map { context.getString(it.explanationRes) }
        val lead = context.getString(R.string.call_explain_lead, displayName)
        return lead + reasons.joinToString(separator = "\n• ", prefix = "\n• ")
    }

    private fun normalize(raw: String?): String =
        raw?.replace(Regex("[\\s()\\-]"), "")?.trim().orEmpty()

    private fun looksInternational(number: String): Boolean =
        number.startsWith("+") && !number.startsWith("+1") && !number.startsWith("+7")

    private fun highRiskPrefix(number: String): Boolean =
        HIGH_RISK_PREFIXES.any { number.startsWith(it) }

    private fun matchesAnyKeyword(text: String, keywords: Array<String>): Boolean =
        text.isNotBlank() && keywords.any { it in text }

    companion object {
        private const val TAG = "PhoneNumberAnalyzer"

        private const val REPEAT_THRESHOLD = 3
        private const val REPEAT_WINDOW_MS = 24L * 60 * 60 * 1000   // 24h

        /**
         * Country prefixes historically used for premium-rate / scam call origins.
         * Heuristic only; combined with other flags before triggering a block.
         */
        private val HIGH_RISK_PREFIXES = arrayOf(
            "+234", // Nigeria
            "+233", // Ghana
            "+225", // Côte d'Ivoire
            "+1900", // US premium
            "+1809", // Caribbean fraud zone
            "+1876", // Jamaica
            "+44844", // UK premium
            "+44871",
        )

        private val FAKE_BANK_KEYWORDS = arrayOf(
            "bank", "банк", "сбер", "kaspi", "halyk", "card", "карта",
            "credit", "loan", "кредит", "счет", "account",
        )
        private val SUPPORT_KEYWORDS = arrayOf(
            "support", "поддержк", "service", "help desk", "helpdesk",
            "tech", "agent", "operator", "оператор", "служба",
        )
        private val SCAM_KEYWORDS = arrayOf(
            "police", "полиц", "tax", "налог", "irs", "court", "суд",
            "fraud", "investigation", "warrant", "arrest", "verification",
            "верифик", "winner", "приз", "lottery", "лотерея",
        )
        private val ROBOCALL_KEYWORDS = arrayOf(
            "auto", "robocall", "press 1", "press one", "нажмите",
            "this is an automated", "автоматическ",
        )
    }
}
