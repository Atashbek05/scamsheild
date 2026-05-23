package com.example.scamshield.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.call.CallAction
import com.example.scamshield.data.call.CallRisk
import com.example.scamshield.engine.call.PhoneNumberAnalyzer
import com.example.scamshield.overlay.CallOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fallback path when ScamShield isn't installed as the system's caller-ID app:
 * listens for PHONE_STATE broadcasts, runs the analyser on RINGING events, and
 * raises the warning overlay if the call is risky.
 *
 * Cannot reject the call (only [ScamShieldCallScreeningService] can), so high-
 * risk calls are surfaced via overlay with one-tap block/report buttons.
 *
 * Requires `android.permission.READ_PHONE_STATE`.  Registered for
 * `android.intent.action.PHONE_STATE` in the manifest.
 */
class IncomingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        val incoming = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        Log.d(TAG, "PHONE_STATE RINGING from ${incoming ?: "<hidden>"}")

        // Debounce duplicate broadcasts (PHONE_STATE fires multiple times per ring)
        val now = System.currentTimeMillis()
        if (now - lastBroadcastAt < DEBOUNCE_MS && incoming == lastBroadcastNumber) return
        lastBroadcastAt = now
        lastBroadcastNumber = incoming

        val appContext = context.applicationContext
        scope.launch {
            try {
                val container = ScamShieldApp.container()
                val analyzer = PhoneNumberAnalyzer(appContext, container.callRepository)
                val analysis = analyzer.analyze(incoming)

                if (analysis.risk >= CallRisk.MEDIUM) {
                    CallOverlayManager.showWarning(appContext, analysis)
                }
                container.callRepository.recordCall(analysis, CallAction.SCREENED)
            } catch (t: Throwable) {
                Log.e(TAG, "Incoming-call analysis failed", t)
            }
        }
    }

    companion object {
        private const val TAG = "IncomingCallReceiver"
        private const val DEBOUNCE_MS = 4_000L

        @Volatile private var lastBroadcastAt: Long = 0L
        @Volatile private var lastBroadcastNumber: String? = null

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
