package com.example.scamshield.data

enum class AnalysisSource { BACKEND, LOCAL, COMBINED }

data class ThreatExplanation(
    val suspiciousKeywords: List<String> = emptyList(),
    val phishingLinks: List<String> = emptyList(),
    val urgencyPatterns: List<String> = emptyList(),
    val socialEngineeringIndicators: List<String> = emptyList(),
    val confidenceScore: Float = 0f,
    val overallReason: String = "",
    val analysisSource: AnalysisSource = AnalysisSource.LOCAL,
)
