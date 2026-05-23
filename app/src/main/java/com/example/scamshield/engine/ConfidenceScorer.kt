package com.example.scamshield.engine

import com.example.scamshield.data.RiskLevel
import com.example.scamshield.data.ThreatCategory
import com.example.scamshield.engine.detectors.LinkScanner

/**
 * Combines backend probability with local signal weights into a unified
 * confidence score and a chosen [ThreatCategory].
 *
 * The backend score is the dominant signal — local detectors only nudge it
 * upward when independent evidence supports a higher score, never downward.
 *
 * Weights (max boost = ~0.25, hard-capped at 0.99):
 *   - link findings:      0.06 per finding, max 0.15
 *   - phishing patterns:  0.05 per match, max 0.12
 *   - otp patterns:       0.06 per match, max 0.15
 *   - banking patterns:   0.05 per match, max 0.12
 *   - urgency patterns:   0.03 per match, max 0.08
 *   - social engineering: 0.04 per match, max 0.10
 *   - suspicious sender:  flat 0.05
 */
object ConfidenceScorer {

    data class Inputs(
        val backendProbability: Float,
        val linkFindings: List<LinkScanner.LinkFinding>,
        val phishing: List<String>,
        val otp: List<String>,
        val banking: List<String>,
        val urgency: List<String>,
        val social: List<String>,
        val senderSuspicious: Boolean,
    )

    data class Output(
        val confidence: Float,
        val category: ThreatCategory,
        val riskLevel: RiskLevel,
        val signalCount: Int,
    )

    fun score(inputs: Inputs): Output {
        var conf = inputs.backendProbability.coerceIn(0f, 1f)

        conf += weightedBoost(inputs.linkFindings.size, per = 0.06f, max = 0.15f)
        conf += weightedBoost(inputs.phishing.size,      per = 0.05f, max = 0.12f)
        conf += weightedBoost(inputs.otp.size,           per = 0.06f, max = 0.15f)
        conf += weightedBoost(inputs.banking.size,       per = 0.05f, max = 0.12f)
        conf += weightedBoost(inputs.urgency.size,       per = 0.03f, max = 0.08f)
        conf += weightedBoost(inputs.social.size,        per = 0.04f, max = 0.10f)
        if (inputs.senderSuspicious) conf += 0.05f

        conf = conf.coerceAtMost(0.99f)

        val category = pickCategory(inputs)
        val risk     = RiskLevel.fromProbability(conf)
        val signals  = inputs.linkFindings.size + inputs.phishing.size + inputs.otp.size +
            inputs.banking.size + inputs.urgency.size + inputs.social.size +
            (if (inputs.senderSuspicious) 1 else 0)

        return Output(conf, category, risk, signals)
    }

    private fun weightedBoost(count: Int, per: Float, max: Float): Float =
        (count * per).coerceAtMost(max)

    private fun pickCategory(i: Inputs): ThreatCategory {
        val scores = mapOf(
            ThreatCategory.PHISHING            to i.phishing.size * 2 + i.linkFindings.size,
            ThreatCategory.OTP_SCAM            to i.otp.size * 3,
            ThreatCategory.FAKE_BANKING        to i.banking.size * 2,
            ThreatCategory.SOCIAL_ENGINEERING  to i.social.size * 2,
            ThreatCategory.URGENCY_BAIT        to i.urgency.size,
            ThreatCategory.LINK_TRAP           to i.linkFindings.size * 2,
        )
        val top = scores.maxByOrNull { it.value }
        return if (top != null && top.value > 0) top.key else ThreatCategory.OTHER
    }
}
