package com.example.scamshield.data.call

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Hot in-memory cache of the most recent call events.  The Call Protection
 * dashboard subscribes to these StateFlows for instant updates without
 * round-tripping through Room.  The DB still receives every event for
 * durable history and analytics.
 */
object CallStore {

    /** Recent ring buffer of analysed calls — newest first, capped at [BUFFER_SIZE]. */
    private val _events = MutableStateFlow<List<CallEvent>>(emptyList())
    val events: StateFlow<List<CallEvent>> = _events.asStateFlow()

    /** Total calls screened since process start. */
    private val _totalScreened = MutableStateFlow(0)
    val totalScreened: StateFlow<Int> = _totalScreened.asStateFlow()

    /** Live count of calls blocked since process start. */
    private val _totalBlocked = MutableStateFlow(0)
    val totalBlocked: StateFlow<Int> = _totalBlocked.asStateFlow()

    /** Cache of suspicious numbers seen this session — drives "repeated unknown" heuristic. */
    private val _suspiciousCache = MutableStateFlow<Map<String, Int>>(emptyMap())
    val suspiciousCache: StateFlow<Map<String, Int>> = _suspiciousCache.asStateFlow()

    /** Currently-displayed incoming-call overlay, or null if none is active. */
    private val _activeAlert = MutableStateFlow<CallEvent?>(null)
    val activeAlert: StateFlow<CallEvent?> = _activeAlert.asStateFlow()

    fun addEvent(event: CallEvent) {
        _events.update { (listOf(event) + it).take(BUFFER_SIZE) }
        _totalScreened.update { it + 1 }
        if (event.action == CallAction.BLOCKED) {
            _totalBlocked.update { it + 1 }
        }
        if (event.risk >= CallRisk.LOW) {
            _suspiciousCache.update { cache ->
                cache + (event.phoneNumber to ((cache[event.phoneNumber] ?: 0) + 1))
            }
        }
    }

    fun setActiveAlert(event: CallEvent?) {
        _activeAlert.value = event
    }

    fun repeatCountFor(number: String): Int = _suspiciousCache.value[number] ?: 0

    private const val BUFFER_SIZE = 50
}
