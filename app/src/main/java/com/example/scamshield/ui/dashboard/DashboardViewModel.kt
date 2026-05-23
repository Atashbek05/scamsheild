package com.example.scamshield.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.AiConnectionState
import com.example.scamshield.data.MonitoringStore
import com.example.scamshield.data.ThreatStore
import com.example.scamshield.data.toDetectedThreat
import com.example.scamshield.network.AiConnectionMonitor
import com.example.scamshield.repository.ThreatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Single source of truth for the live dashboard.
 *
 * Aggregates: AI connectivity, live scan event, recent scans, persisted threat
 * counts, and the most-recent threats list. The dashboard composable just
 * collects [state] and renders.
 */
class DashboardViewModel(
    private val threatRepo: ThreatRepository,
) : ViewModel() {

    data class State(
        val aiState: AiConnectionState = AiConnectionState.Idle,
        val totalScanned: Int = 0,
        val totalThreats: Int = 0,
        val highRiskCount: Int = 0,
        val threatsToday: Int = 0,
        val recentScansActive: Boolean = false,
        val activeServiceCount: Int = 0,
    )

    private val today: Long get() = System.currentTimeMillis() - 24L * 60 * 60 * 1000

    val state: StateFlow<State> = combine(
        ThreatStore.aiConnectionState,
        ThreatStore.totalScanned,
        threatRepo.observeCount(),
        threatRepo.observeHighRiskCount(),
        threatRepo.observeCountSince(today),
    ) { ai, scanned, total, high, today ->
        State(
            aiState = ai,
            totalScanned = scanned,
            totalThreats = total,
            highRiskCount = high,
            threatsToday = today,
        )
    }.combine(MonitoringStore.activeServices) { s, services ->
        s.copy(activeServiceCount = services.size)
    }.combine(MonitoringStore.liveEvent) { s, live ->
        s.copy(recentScansActive = live != null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    val liveEvent = MonitoringStore.liveEvent
    val recentScans = MonitoringStore.recentScans
    val recentThreatsFlow = threatRepo.observeRecent(8).map { list -> list.map { it.toDetectedThreat() } }

    fun runAiTest() {
        AiConnectionMonitor.runTest(viewModelScope)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(ScamShieldApp.container().threatRepository)
            }
        }
    }
}
