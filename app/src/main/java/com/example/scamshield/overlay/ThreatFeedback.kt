package com.example.scamshield.overlay

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.scamshield.data.settings.SettingsRepository
import com.example.scamshield.data.settings.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fires haptic and audio feedback when the scam overlay card appears.
 * Vibration intensity scales with threat probability; sound only fires
 * for critical-level threats (≥ 90 %) when the user enables it.
 *
 * Caches [UserSettings] by collecting the DataStore flow once on first use,
 * so [fire] never blocks the main thread.
 */
internal object ThreatFeedback {

    @Volatile private var cached: UserSettings = UserSettings()
    private val scope      = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val collecting = AtomicBoolean(false)

    /** Start observing settings the first time this object is used. */
    private fun ensureCollecting(context: Context) {
        if (!collecting.compareAndSet(false, true)) return
        scope.launch {
            SettingsRepository.get(context).settings.collect { cached = it }
        }
    }

    /**
     * Called from the main thread in [OverlayManager] after the card is added
     * to the window. [probability] is the raw scam score in [0, 1].
     */
    fun fire(appContext: Context, probability: Float) {
        ensureCollecting(appContext)
        val s = cached
        if (s.vibrationEnabled) vibrate(appContext, probability)
        if (s.soundOnCritical && probability >= CRITICAL_THRESHOLD) playSound(appContext)
    }

    // ── Vibration ─────────────────────────────────────────────────────────────────

    private fun vibrate(context: Context, probability: Float) {
        val vibrator = getVibrator(context) ?: return
        val pattern: LongArray = when {
            probability >= CRITICAL_THRESHOLD -> longArrayOf(0L, 500L, 200L, 500L)
            probability >= HIGH_THRESHOLD     -> longArrayOf(0L, 300L)
            else                              -> longArrayOf(0L, 100L)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun getVibrator(context: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }.getOrNull()

    // ── Sound ─────────────────────────────────────────────────────────────────────

    private fun playSound(context: Context) {
        runCatching {
            val uri      = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        }
    }

    // ── Thresholds (mirror RiskLevel) ─────────────────────────────────────────────

    private const val CRITICAL_THRESHOLD = 0.90f
    private const val HIGH_THRESHOLD     = 0.70f
}
