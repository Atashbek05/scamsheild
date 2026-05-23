package com.example.scamshield.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.call.CallStore
import com.example.scamshield.data.db.BlockedNumberEntity
import com.example.scamshield.data.db.CallEntity
import com.example.scamshield.data.db.TopScamCallerRow
import com.example.scamshield.repository.CallRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Call Protection dashboard.  Combines durable history from
 * [CallRepository] with the live in-memory feed from [CallStore].
 */
class CallProtectionViewModel(
    private val repository: CallRepository,
) : ViewModel() {

    private val dayMillis = 24L * 60 * 60 * 1000

    val recentCalls: StateFlow<List<CallEntity>> = repository.observeRecent(limit = 30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalScreened: StateFlow<Int> = repository.observeTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val riskyCount: StateFlow<Int> = repository.observeRiskyCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val blockedCount: StateFlow<Int> = repository.observeBlockedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val today: StateFlow<Int> = repository
        .observeCountSince(System.currentTimeMillis() - dayMillis)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val todayRisky: StateFlow<Int> = repository
        .observeRiskyCountSince(System.currentTimeMillis() - dayMillis)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val topCallers: StateFlow<List<TopScamCallerRow>> = repository.observeTopScamCallers(limit = 5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val blocked: StateFlow<List<BlockedNumberEntity>> = repository.observeBlocked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val liveBlocked: StateFlow<Int> = CallStore.totalBlocked
    val liveScreened: StateFlow<Int> = CallStore.totalScreened

    fun block(number: String, reason: String = "Manual block") {
        if (number.isBlank()) return
        viewModelScope.launch { repository.block(number, reason) }
    }

    fun unblock(number: String) {
        viewModelScope.launch { repository.unblock(number) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun deleteCall(id: Long) {
        viewModelScope.launch { repository.deleteCall(id) }
    }

    fun parseFlags(stored: String) = repository.parseFlags(stored)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CallProtectionViewModel(ScamShieldApp.container().callRepository)
            }
        }
    }
}
