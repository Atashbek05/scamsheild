package com.example.scamshield.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.db.TrustedContactEntity
import com.example.scamshield.repository.CallRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrustedContactsViewModel(private val repo: CallRepository) : ViewModel() {

    val trusted: StateFlow<List<TrustedContactEntity>> = repo.observeTrusted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addContact(number: String, name: String) {
        viewModelScope.launch { repo.trust(number.trim(), name.trim()) }
    }

    fun removeContact(number: String) {
        viewModelScope.launch { repo.untrust(number) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { TrustedContactsViewModel(ScamShieldApp.container().callRepository) }
        }
    }
}
