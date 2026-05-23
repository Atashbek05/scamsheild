package com.example.scamshield.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "scamshield_settings")

/**
 * Persists [UserSettings] in a DataStore Preferences file.
 *
 * UI layers should collect [settings] as a Flow and call the typed `set*`
 * methods to mutate individual fields — never the raw Preferences.
 */
class SettingsRepository private constructor(context: Context) {

    private val ds = context.applicationContext.dataStore

    private object Keys {
        val DARK_MODE             = stringPreferencesKey("dark_mode")
        val SENSITIVITY           = stringPreferencesKey("sensitivity")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notif_enabled")
        val NOTIF_SOUND           = booleanPreferencesKey("notif_sound")
        val NOTIF_VIBRATE         = booleanPreferencesKey("notif_vibrate")
        val OVERLAY_ENABLED       = booleanPreferencesKey("overlay_enabled")
        val OVERLAY_AUTODISMISS   = intPreferencesKey("overlay_autodismiss")
        val OVERLAY_POSITION      = stringPreferencesKey("overlay_position")
        val SCAN_MODE             = stringPreferencesKey("scan_mode")
        val BACKEND_URL           = stringPreferencesKey("backend_url")
        val AUTO_SCAN             = booleanPreferencesKey("auto_scan")
        val ONBOARDING_COMPLETE   = booleanPreferencesKey("onboarding_complete")
        val ACTIVE_PROTECTION     = booleanPreferencesKey("active_protection")
    }

    val settings: Flow<UserSettings> = ds.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { p -> p.toUserSettings() }

    private fun Preferences.toUserSettings(): UserSettings {
        return UserSettings(
            darkMode = runCatching {
                DarkMode.valueOf(this[Keys.DARK_MODE] ?: DarkMode.ALWAYS_DARK_CYBER.name)
            }.getOrDefault(DarkMode.ALWAYS_DARK_CYBER),
            sensitivity = runCatching {
                Sensitivity.valueOf(this[Keys.SENSITIVITY] ?: Sensitivity.BALANCED.name)
            }.getOrDefault(Sensitivity.BALANCED),
            notificationsEnabled = this[Keys.NOTIFICATIONS_ENABLED] ?: true,
            notificationSound    = this[Keys.NOTIF_SOUND] ?: true,
            notificationVibrate  = this[Keys.NOTIF_VIBRATE] ?: true,
            overlayEnabled       = this[Keys.OVERLAY_ENABLED] ?: true,
            overlayAutoDismissSec = this[Keys.OVERLAY_AUTODISMISS] ?: 7,
            overlayPosition = runCatching {
                OverlayPosition.valueOf(this[Keys.OVERLAY_POSITION] ?: OverlayPosition.TOP.name)
            }.getOrDefault(OverlayPosition.TOP),
            scanMode = runCatching {
                ScanMode.valueOf(this[Keys.SCAN_MODE] ?: ScanMode.HYBRID.name)
            }.getOrDefault(ScanMode.HYBRID),
            backendUrl           = this[Keys.BACKEND_URL] ?: UserSettings.DEFAULT_BACKEND_URL,
            autoScanEnabled      = this[Keys.AUTO_SCAN] ?: true,
            onboardingComplete   = this[Keys.ONBOARDING_COMPLETE] ?: false,
            activeProtection     = this[Keys.ACTIVE_PROTECTION] ?: true,
        )
    }

    suspend fun setDarkMode(value: DarkMode)              = ds.edit { it[Keys.DARK_MODE] = value.name }
    suspend fun setSensitivity(value: Sensitivity)        = ds.edit { it[Keys.SENSITIVITY] = value.name }
    suspend fun setNotificationsEnabled(value: Boolean)   = ds.edit { it[Keys.NOTIFICATIONS_ENABLED] = value }
    suspend fun setNotificationSound(value: Boolean)      = ds.edit { it[Keys.NOTIF_SOUND] = value }
    suspend fun setNotificationVibrate(value: Boolean)    = ds.edit { it[Keys.NOTIF_VIBRATE] = value }
    suspend fun setOverlayEnabled(value: Boolean)         = ds.edit { it[Keys.OVERLAY_ENABLED] = value }
    suspend fun setOverlayAutoDismissSec(value: Int)      = ds.edit { it[Keys.OVERLAY_AUTODISMISS] = value }
    suspend fun setOverlayPosition(value: OverlayPosition)= ds.edit { it[Keys.OVERLAY_POSITION] = value.name }
    suspend fun setScanMode(value: ScanMode)              = ds.edit { it[Keys.SCAN_MODE] = value.name }
    suspend fun setBackendUrl(value: String)              = ds.edit { it[Keys.BACKEND_URL] = value }
    suspend fun setAutoScanEnabled(value: Boolean)        = ds.edit { it[Keys.AUTO_SCAN] = value }
    suspend fun setOnboardingComplete(value: Boolean)     = ds.edit { it[Keys.ONBOARDING_COMPLETE] = value }
    suspend fun setActiveProtection(value: Boolean)       = ds.edit { it[Keys.ACTIVE_PROTECTION] = value }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
    }
}
