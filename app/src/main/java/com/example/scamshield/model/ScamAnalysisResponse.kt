package com.example.scamshield.model

import com.google.gson.annotations.SerializedName

/**
 * Response body returned by the FastAPI /analyze endpoint.
 *
 * @param scamProbability Float in [0.0, 1.0] — confidence the text is a scam.
 * @param label           Human-readable verdict: "scam" or "safe".
 * @param suspiciousKeywords List of words/phrases that contributed to the score.
 */
data class ScamAnalysisResponse(
    @SerializedName("scam_probability") val scamProbability: Float,
    @SerializedName("label") val label: String,
    @SerializedName("suspicious_keywords") val suspiciousKeywords: List<String>
)
