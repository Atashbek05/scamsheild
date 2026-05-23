package com.example.scamshield.ui.simulator

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.DetectedThreat
import com.example.scamshield.data.ThreatStore
import com.example.scamshield.engine.ExplainabilityEngine
import com.example.scamshield.overlay.OverlayData
import com.example.scamshield.overlay.OverlayManager
import com.example.scamshield.overlay.ThreatLevel
import com.example.scamshield.repository.ScamAnalysisRepository
import com.example.scamshield.repository.ThreatRepository
import com.example.scamshield.simulator.ScamSamples
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Powers the in-app scam simulator screen.
 *
 * The user can pick a built-in sample or type their own message. The simulator
 * runs the same analysis pipeline that production traffic uses, then records
 * the result in [ThreatStore] and persists it via [ThreatRepository] so the
 * dashboard and history reflect the test immediately.
 *
 * Overlay rendering is gated on `showOverlay`; defaults to true so users get
 * the full visual experience.
 */
class SimulatorViewModel(
    private val context: Context,
    private val threatRepo: ThreatRepository,
    private val analysis: ScamAnalysisRepository,
) : ViewModel() {

    private val TAG = "SimulatorVM"

    sealed class State {
        data object Idle : State()
        data object Loading : State()
        data class Success(
            val message: String,
            val sender: String,
            val result: ExplainabilityEngine.Result,
        ) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _showOverlay = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()

    val samples: List<ScamSamples.Sample> = ScamSamples.all

    fun setShowOverlay(value: Boolean) { _showOverlay.value = value }

    fun runSample(sample: ScamSamples.Sample) {
        runAnalysis(message = sample.message, sender = sample.sender)
    }

    fun runCustom(message: String, sender: String) {
        if (message.isBlank()) {
            _state.value = State.Error("Type a message first")
            return
        }
        runAnalysis(message, sender)
    }

    private fun runAnalysis(message: String, sender: String) {
        _state.value = State.Loading
        viewModelScope.launch {
            // Try backend; fall back to local analysis if offline so the simulator works anywhere.
            val (backendProb, backendKeywords, backendLabel) = withContext(Dispatchers.IO) {
                analysis.analyzeText(message, "simulator").fold(
                    onSuccess = { Triple(it.scamProbability, it.suspiciousKeywords, it.label) },
                    onFailure = {
                        Log.w(TAG, "Backend unreachable — running local-only", it)
                        Triple(0f, emptyList(), "unknown")
                    },
                )
            }

            val result = withContext(Dispatchers.Default) {
                ExplainabilityEngine.analyze(
                    message = message,
                    senderRaw = sender,
                    backendKeywords = backendKeywords,
                    backendProbability = backendProb,
                )
            }

            ThreatStore.incrementScanned()
            val threat = DetectedThreat(
                sourcePackage = "com.example.scamshield.simulator",
                messagePreview = if (message.length > 200) "${message.take(200)}…" else message,
                probability = result.finalProbability,
                keywords = backendKeywords,
                explanation = result.explanation,
                appLabel = "Simulator",
                category = result.category,
                backendLabel = backendLabel,
            )
            ThreatStore.addThreat(threat)
            threatRepo.insert(threat, appLabel = "Simulator", category = result.category, backendLabel = backendLabel)

            if (_showOverlay.value && result.finalProbability >= 0.40f) {
                showOverlayCard(threat, result)
            }

            _state.value = State.Success(message = message, sender = sender, result = result)
        }
    }

    private fun showOverlayCard(threat: DetectedThreat, result: ExplainabilityEngine.Result) {
        val tl = if (result.finalProbability >= 0.70f) ThreatLevel.RED else ThreatLevel.YELLOW
        val overlay = OverlayData(
            probability    = result.finalProbability,
            label          = "scam",
            keywords       = threat.keywords + result.linkReport.findings.map { it.host },
            messagePreview = threat.messagePreview,
            packageName    = "com.example.scamshield.simulator",
            sourceAppName  = "Simulator",
            threatLevel    = tl,
        )
        OverlayManager.showWarning(context, overlay)
    }

    fun reset() { _state.value = State.Idle }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = ScamShieldApp.container()
                SimulatorViewModel(
                    context = ScamShieldApp.appContext(),
                    threatRepo = app.threatRepository,
                    analysis = app.analysisRepository,
                )
            }
        }
    }
}
