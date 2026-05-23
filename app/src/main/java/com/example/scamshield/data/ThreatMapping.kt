package com.example.scamshield.data

import com.example.scamshield.data.db.ThreatEntity

/**
 * Conversions between the runtime data class [DetectedThreat] / [ThreatExplanation]
 * and the persisted [ThreatEntity]. Centralising this here keeps services, view
 * models, and the PDF generator agnostic of which layer owns the data.
 *
 * Lists are joined with a non-printing separator so they round-trip without
 * collisions with normal text content.
 */
private const val LIST_SEP = " ▸ "

fun List<String>.joinForDb(): String = joinToString(LIST_SEP)
fun String.splitFromDb(): List<String> =
    if (isBlank()) emptyList() else split(LIST_SEP).filter { it.isNotBlank() }

fun DetectedThreat.toEntity(
    appLabel: String,
    category: ThreatCategory,
    backendLabel: String,
): ThreatEntity {
    val ex = explanation ?: ThreatExplanation()
    return ThreatEntity(
        sourcePackage     = sourcePackage,
        appLabel          = appLabel,
        messagePreview    = messagePreview,
        probability       = probability,
        riskLevel         = RiskLevel.fromProbability(probability).name,
        category          = category.name,
        backendLabel      = backendLabel,
        keywords          = keywords.joinForDb(),
        phishingLinks     = ex.phishingLinks.joinForDb(),
        urgencyPatterns   = ex.urgencyPatterns.joinForDb(),
        socialIndicators  = ex.socialEngineeringIndicators.joinForDb(),
        explanation       = ex.overallReason,
        analysisSource    = ex.analysisSource.name,
        timestamp         = timestamp,
    )
}

fun ThreatEntity.toDetectedThreat(): DetectedThreat = DetectedThreat(
    id              = id,
    sourcePackage   = sourcePackage,
    messagePreview  = messagePreview,
    probability     = probability,
    keywords        = keywords.splitFromDb(),
    timestamp       = timestamp,
    explanation     = ThreatExplanation(
        suspiciousKeywords          = keywords.splitFromDb(),
        phishingLinks               = phishingLinks.splitFromDb(),
        urgencyPatterns             = urgencyPatterns.splitFromDb(),
        socialEngineeringIndicators = socialIndicators.splitFromDb(),
        confidenceScore             = probability,
        overallReason               = explanation,
        analysisSource              = runCatching { AnalysisSource.valueOf(analysisSource) }
            .getOrDefault(AnalysisSource.LOCAL),
    ),
)

fun ThreatEntity.riskLevelEnum(): RiskLevel =
    runCatching { RiskLevel.valueOf(riskLevel) }
        .getOrDefault(RiskLevel.fromProbability(probability))

fun ThreatEntity.categoryEnum(): ThreatCategory = ThreatCategory.fromString(category)
