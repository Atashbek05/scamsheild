package com.example.scamshield.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.settings.DarkMode
import com.example.scamshield.data.settings.OverlayPosition
import com.example.scamshield.data.settings.ScanMode
import com.example.scamshield.data.settings.Sensitivity
import com.example.scamshield.data.settings.SettingsRepository
import com.example.scamshield.data.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    fun setDarkMode(value: DarkMode) = viewModelScope.launch { repo.setDarkMode(value) }
    fun setSensitivity(value: Sensitivity) = viewModelScope.launch { repo.setSensitivity(value) }
    fun setNotificationsEnabled(value: Boolean) = viewModelScope.launch { repo.setNotificationsEnabled(value) }
    fun setNotificationSound(value: Boolean) = viewModelScope.launch { repo.setNotificationSound(value) }
    fun setNotificationVibrate(value: Boolean) = viewModelScope.launch { repo.setNotificationVibrate(value) }
    fun setOverlayEnabled(value: Boolean) = viewModelScope.launch { repo.setOverlayEnabled(value) }
    fun setOverlayAutoDismiss(value: Int) = viewModelScope.launch { repo.setOverlayAutoDismissSec(value) }
    fun setOverlayPosition(value: OverlayPosition) = viewModelScope.launch { repo.setOverlayPosition(value) }
    fun setScanMode(value: ScanMode) = viewModelScope.launch { repo.setScanMode(value) }
    fun setBackendUrl(value: String) = viewModelScope.launch { repo.setBackendUrl(value) }
    fun setAutoScanEnabled(value: Boolean) = viewModelScope.launch { repo.setAutoScanEnabled(value) }
    fun setActiveProtection(value: Boolean) = viewModelScope.launch { repo.setActiveProtection(value) }
    fun completeOnboarding() = viewModelScope.launch { repo.setOnboardingComplete(true) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(ScamShieldApp.container().settingsRepository)
            }
        }
    }
}
