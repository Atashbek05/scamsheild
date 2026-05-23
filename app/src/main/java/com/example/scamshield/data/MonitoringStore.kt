package com.example.scamshield.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory state for the live monitoring panel.
 *
 * - [liveEvent]     — the scan currently in-progress (null = idle)
 * - [recentScans]   — last 30 completed scans (newest first)
 * - [activeServices] — which Android services have connected and are running
 *
 * All mutations are thread-safe: StateFlow.update uses compareAndSet internally.
 */
object MonitoringStore {

    private val _liveEvent = MutableStateFlow<MonitoringEvent?>(null)
    val liveEvent: StateFlow<MonitoringEvent?> = _liveEvent.asStateFlow()

    private val _recentScans = MutableStateFlow<List<MonitoringEvent>>(emptyList())
    val recentScans: StateFlow<List<MonitoringEvent>> = _recentScans.asStateFlow()

    private val _activeServices = MutableStateFlow<Set<MonitorSource>>(emptySet())
    val activeServices: StateFlow<Set<MonitorSource>> = _activeServices.asStateFlow()

    /** Publish a new in-progress scan or overwrite the current one. */
    fun pushLiveEvent(event: MonitoringEvent) {
        _liveEvent.value = event
    }

    /** Remove the in-progress event (called when a service shuts down). */
    fun clearLiveEvent() {
        _liveEvent.value = null
    }

    /**
     * Finalise a scan: clears [liveEvent] if it belongs to [event] and prepends
     * [event] to [recentScans].  Safe to call from any thread.
     */
    fun commitScan(event: MonitoringEvent) {
        _liveEvent.update { current -> if (current?.id == event.id) null else current }
        _recentScans.update { (listOf(event) + it).take(30) }
    }

    fun setServiceActive(source: MonitorSource, active: Boolean) {
        _activeServices.update { if (active) it + source else it - source }
    }
}
