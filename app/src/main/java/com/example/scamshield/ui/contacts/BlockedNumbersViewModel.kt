package com.example.scamshield.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.db.BlockedNumberEntity
import com.example.scamshield.repository.CallRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlockedNumbersViewModel(private val repo: CallRepository) : ViewModel() {

    val blocked: StateFlow<List<BlockedNumberEntity>> = repo.observeBlocked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addNumber(number: String) {
        viewModelScope.launch { repo.block(number.trim(), "MANUAL") }
    }

    fun removeNumber(number: String) {
        viewModelScope.launch { repo.unblock(number) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { BlockedNumbersViewModel(ScamShieldApp.container().callRepository) }
        }
    }
}
