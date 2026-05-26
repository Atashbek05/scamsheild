package com.example.scamshield

import android.content.Context
import com.example.scamshield.data.settings.SettingsRepository
import com.example.scamshield.repository.CallRepository
import com.example.scamshield.repository.FeedbackRepository
import com.example.scamshield.repository.ScamAnalysisRepository
import com.example.scamshield.repository.ThreatRepository

/**
 * Lightweight DI container — manual factory-style wiring without a framework.
 *
 * One instance lives on the [ScamShieldApp]; ViewModels and services pull
 * collaborators from here instead of building their own. Keeps test-time
 * substitution trivial (just override the [INSTANCE] before any Activity starts).
 */
class AppContainer(context: Context) {
    val threatRepository: ThreatRepository = ThreatRepository.get(context)
    val settingsRepository: SettingsRepository = SettingsRepository.get(context)
    val analysisRepository: ScamAnalysisRepository = ScamAnalysisRepository()
    val callRepository: CallRepository = CallRepository.get(context)
    val feedbackRepository: FeedbackRepository = FeedbackRepository.get(context)
}
