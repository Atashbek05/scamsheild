package com.example.scamshield.data.settings

import androidx.annotation.StringRes
import com.example.scamshield.R

/**
 * App-wide user preferences exposed by [SettingsRepository].
 *
 * All fields are immutable so the UI can collect [SettingsRepository.settings]
 * as a [kotlinx.coroutines.flow.StateFlow] of [UserSettings] and re-compose only
 * when something actually changes.
 */
data class UserSettings(
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val sensitivity: Sensitivity = Sensitivity.BALANCED,
    val notificationsEnabled: Boolean = true,
    val notificationSound: Boolean = true,
    val notificationVibrate: Boolean = true,
    val overlayEnabled: Boolean = true,
    val overlayAutoDismissSec: Int = 7,
    val overlayPosition: OverlayPosition = OverlayPosition.TOP,
    val scanMode: ScanMode = ScanMode.HYBRID,
    val backendUrl: String = DEFAULT_BACKEND_URL,
    val autoScanEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
    val activeProtection: Boolean = true,
) {
    companion object {
        const val DEFAULT_BACKEND_URL = "https://scamsheil-backend.onrender.com/"
    }
}

enum class DarkMode { SYSTEM, DARK, ALWAYS_DARK_CYBER }

/**
 * Sensitivity gates the probability threshold below which an overlay/notification fires.
 * Lower thresholds trigger more often (more false positives but better recall).
 */
enum class Sensitivity(
    val label: String,
    val overlayThreshold: Float,
    val notifyThreshold: Float,
    @StringRes val labelRes: Int,
) {
    LOW("Low",           0.85f, 0.75f, R.string.sensitivity_low),
    BALANCED("Balanced", 0.70f, 0.55f, R.string.sensitivity_balanced),
    HIGH("High",         0.55f, 0.40f, R.string.sensitivity_high),
    PARANOID("Paranoid", 0.40f, 0.30f, R.string.sensitivity_paranoid),
}

enum class OverlayPosition(
    @StringRes val labelRes: Int,
) {
    TOP(R.string.overlay_position_top),
    CENTER(R.string.overlay_position_center),
    BOTTOM(R.string.overlay_position_bottom),
}

enum class ScanMode(
    val label: String,
    val description: String,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
) {
    BACKEND_ONLY(
        "Backend AI",
        "Only flag what the AI backend marks as scam",
        R.string.scan_mode_backend,
        R.string.scan_mode_backend_desc,
    ),
    LOCAL_ONLY(
        "Local Engine",
        "Use only on-device pattern detectors (offline)",
        R.string.scan_mode_local,
        R.string.scan_mode_local_desc,
    ),
    HYBRID(
        "Hybrid",
        "AI backend + local detectors (recommended)",
        R.string.scan_mode_hybrid,
        R.string.scan_mode_hybrid_desc,
    ),
}
