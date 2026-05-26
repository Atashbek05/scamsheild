package com.example.scamshield.engine

import android.content.Context
import android.util.Log
import com.example.scamshield.R
import com.example.scamshield.util.logD
import com.example.scamshield.data.AnalysisSource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.example.scamshield.data.RiskLevel
import com.example.scamshield.data.ThreatCategory
import com.example.scamshield.data.ThreatExplanation
import com.example.scamshield.engine.detectors.FakeBankingDetector
import com.example.scamshield.engine.detectors.LinkScanner
import com.example.scamshield.engine.detectors.OtpScamDetector
import com.example.scamshield.engine.detectors.PhishingDetector
import com.example.scamshield.engine.detectors.SenderReputation
import com.example.scamshield.engine.detectors.SocialEngineeringDetector
import com.example.scamshield.engine.detectors.UrgencyDetector

/**
 * Aggregates the full detection pipeline:
 *
 *   1. Run every local detector on the message
 *   2. Score the combination with [ConfidenceScorer]
 *   3. Build a [ThreatExplanation] for the UI and PDF report
 *   4. Surface the category + risk-level so callers can pick the right visuals
 *
 * Pure CPU-bound — safe to call from a background dispatcher; no I/O.
 */
object ExplainabilityEngine {

    private const val TAG = "ExplainEngine"

    /** Public bundle so services and the simulator can share a single call site. */
    data class Result(
        val explanation: ThreatExplanation,
        val category: ThreatCategory,
        val riskLevel: RiskLevel,
        val finalProbability: Float,
        val signalCount: Int,
        val linkReport: LinkScanner.Report,
    )

    fun analyze(
        context: Context,
        message: String,
        senderRaw: String,
        backendKeywords: List<String>,
        backendProbability: Float,
        backendOffline: Boolean = false,
    ): Result = try {
        analyzeInternal(context, message, senderRaw, backendKeywords, backendProbability, backendOffline)
    } catch (e: Exception) {
        Log.e(TAG, "Unexpected error in analyze(): ${e.message}", e)
        FirebaseCrashlytics.getInstance().recordException(e)
        throw e
    }

    private fun analyzeInternal(
        context: Context,
        message: String,
        senderRaw: String,
        backendKeywords: List<String>,
        backendProbability: Float,
        backendOffline: Boolean,
    ): Result {
        val lower = message.lowercase()

        val phishing  = PhishingDetector.detect(lower)
        val otp       = OtpScamDetector.detect(lower)
        val banking   = FakeBankingDetector.detect(lower)
        val urgency   = UrgencyDetector.detect(lower)
        val social    = SocialEngineeringDetector.detect(lower)
        val linkRep   = LinkScanner.scan(message)
        val senderRep = SenderReputation.score(senderRaw)

        // When backend is offline, don't use its probability as the base score —
        // local detectors are the sole source and their boosts accumulate from 0.
        val effectiveProbability = if (backendOffline) 0.0f else backendProbability

        val scored = ConfidenceScorer.score(
            ConfidenceScorer.Inputs(
                backendProbability = effectiveProbability,
                linkFindings = linkRep.findings,
                phishing = phishing,
                otp = otp,
                banking = banking,
                urgency = urgency,
                social = social,
                senderSuspicious = senderRep.tier == SenderReputation.Tier.SUSPICIOUS,
            )
        )

        val mergedUrgency = (urgency + senderRep.reasons.filter { it.contains("ALL-CAPS") }).distinct()
        val mergedSocial  = (social + phishing + banking +
            senderRep.reasons.filter { !it.contains("ALL-CAPS") }).distinct()

        val source = if (backendOffline) AnalysisSource.LOCAL
                     else pickSource(backendKeywords, hasLocal = scored.signalCount > 0)

        val baseReason = buildReason(
            context = context,
            category = scored.category,
            risk = scored.riskLevel,
            confidence = scored.confidence,
            phishingCount = phishing.size,
            otpCount = otp.size,
            bankingCount = banking.size,
            urgencyCount = urgency.size,
            socialCount = social.size,
            linkCount = linkRep.findings.size,
            senderSuspicious = senderRep.tier == SenderReputation.Tier.SUSPICIOUS,
        )
        val overallReason = if (backendOffline)
            "$baseReason ${context.getString(R.string.explain_ai_offline)}"
        else baseReason

        val explanation = ThreatExplanation(
            suspiciousKeywords          = if (backendOffline) emptyList() else backendKeywords,
            phishingLinks               = linkRep.findings.map { it.display },
            urgencyPatterns             = mergedUrgency,
            socialEngineeringIndicators = mergedSocial,
            confidenceScore             = scored.confidence,
            overallReason               = overallReason,
            analysisSource              = source,
        )

        logD(
            TAG,
            "Analyzed | cat=${scored.category} risk=${scored.riskLevel} " +
                "conf=${"%.2f".format(scored.confidence)} " +
                "signals=${scored.signalCount} " +
                "(phish=${phishing.size} otp=${otp.size} bank=${banking.size} " +
                "urg=${urgency.size} soc=${social.size} links=${linkRep.findings.size})"
        )

        return Result(
            explanation = explanation,
            category = scored.category,
            riskLevel = scored.riskLevel,
            finalProbability = scored.confidence,
            signalCount = scored.signalCount,
            linkReport = linkRep,
        )
    }

    /**
     * Backward-compat shim — the old [explain] signature is still used by
     * services that haven't been re-routed yet. New code should call [analyze].
     */
    @Suppress("unused")
    fun explain(
        context: Context,
        message: String,
        backendKeywords: List<String>,
        probability: Float,
    ): ThreatExplanation = analyze(
        context = context,
        message = message,
        senderRaw = "",
        backendKeywords = backendKeywords,
        backendProbability = probability,
    ).explanation

    private fun pickSource(backendKeywords: List<String>, hasLocal: Boolean): AnalysisSource = when {
        backendKeywords.isNotEmpty() && hasLocal -> AnalysisSource.COMBINED
        backendKeywords.isNotEmpty()             -> AnalysisSource.BACKEND
        else                                     -> AnalysisSource.LOCAL
    }

    private fun buildReason(
        context: Context,
        category: ThreatCategory,
        risk: RiskLevel,
        confidence: Float,
        phishingCount: Int,
        otpCount: Int,
        bankingCount: Int,
        urgencyCount: Int,
        socialCount: Int,
        linkCount: Int,
        senderSuspicious: Boolean,
    ): String {
        val pct = (confidence * 100).toInt()
        val res = context.resources
        val parts = buildList {
            if (linkCount > 0)       add(res.getQuantityString(R.plurals.explain_signal_links, linkCount, linkCount))
            if (phishingCount > 0)   add(res.getQuantityString(R.plurals.explain_signal_phishing_cues, phishingCount, phishingCount))
            if (otpCount > 0)        add(context.getString(R.string.explain_signal_otp))
            if (bankingCount > 0)    add(context.getString(R.string.explain_signal_banking))
            if (urgencyCount > 0)    add(context.getString(R.string.explain_signal_urgency))
            if (socialCount > 0)     add(context.getString(R.string.explain_signal_social))
            if (senderSuspicious)    add(context.getString(R.string.explain_signal_sender))
        }
        val riskLabel = context.getString(risk.labelRes)
        if (parts.isEmpty())
            return context.getString(R.string.explain_ai_confidence_patterns, pct, riskLabel)
        val categoryLabel = context.getString(category.labelRes)
        return context.getString(R.string.explain_ai_confidence_detected, pct, categoryLabel, parts.joinToString(", "), riskLabel)
    }
}
