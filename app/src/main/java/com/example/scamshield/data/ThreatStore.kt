package com.example.scamshield.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ── AI connectivity ───────────────────────────────────────────────────────────

sealed class AiConnectionState {
    object Idle       : AiConnectionState()
    object Connecting : AiConnectionState()
    object Online     : AiConnectionState()
    object Offline    : AiConnectionState()
    data class Error(val message: String) : AiConnectionState()
}

// ── Test result ───────────────────────────────────────────────────────────────

data class AiTestResult(
    val scamProbability: Float,
    val label: String,
    val keywords: List<String>,
)

sealed class AiTestState {
    object Idle    : AiTestState()
    object Loading : AiTestState()
    data class Success(val result: AiTestResult) : AiTestState()
    data class Error(val message: String)        : AiTestState()
}

// ── Activity log ──────────────────────────────────────────────────────────────

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
)

// ── Threat ────────────────────────────────────────────────────────────────────

data class DetectedThreat(
    val id: Long = System.currentTimeMillis(),
    val sourcePackage: String,
    val messagePreview: String,
    val probability: Float,
    val keywords: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val explanation: ThreatExplanation? = null,
    val appLabel: String? = null,
    val category: ThreatCategory = ThreatCategory.OTHER,
    val backendLabel: String = "",
)

// ── Store ─────────────────────────────────────────────────────────────────────

object ThreatStore {

    private val _threats = MutableStateFlow<List<DetectedThreat>>(emptyList())
    val threats: StateFlow<List<DetectedThreat>> = _threats.asStateFlow()

    private val _totalScanned = MutableStateFlow(0)
    val totalScanned: StateFlow<Int> = _totalScanned.asStateFlow()

    private val _aiConnectionState = MutableStateFlow<AiConnectionState>(AiConnectionState.Idle)
    val aiConnectionState: StateFlow<AiConnectionState> = _aiConnectionState.asStateFlow()

    private val _aiTestState = MutableStateFlow<AiTestState>(AiTestState.Idle)
    val aiTestState: StateFlow<AiTestState> = _aiTestState.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun addThreat(threat: DetectedThreat) {
        _threats.update { current -> (listOf(threat) + current).take(20) }
    }

    fun incrementScanned() {
        _totalScanned.update { it + 1 }
    }

    // Kept for backward-compat — services still call setAiConnected(bool)
    fun setAiConnected(connected: Boolean) {
        _aiConnectionState.value =
            if (connected) AiConnectionState.Online else AiConnectionState.Offline
    }

    fun setAiConnectionState(state: AiConnectionState) {
        _aiConnectionState.value = state
    }

    fun setAiTestState(state: AiTestState) {
        _aiTestState.value = state
    }

    fun addLog(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(level = level, tag = tag, message = message)
        _logs.update { (listOf(entry) + it).take(50) }
    }
}