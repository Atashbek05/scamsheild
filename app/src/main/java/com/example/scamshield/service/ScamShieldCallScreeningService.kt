package com.example.scamshield.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.scamshield.util.logD
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
 * System hook that lets ScamShield inspect every incoming call before it rings.
 *
 * The framework gives us a tight window (≤5s on most OEMs) to call
 * [respondToCall] with either an "allow" or "block" verdict, so the analyser
 * is local and synchronous: number-shape heuristics + caller-ID keyword scan +
 * blocked-number lookup against Room.
 *
 * Behaviour:
 *   • TRUSTED / SAFE / LOW     → respondAllow + log to history
 *   • MEDIUM                   → respondAllow but raise overlay warning
 *   • HIGH                     → respondAllow + RED overlay
 *   • CRITICAL or blocked DB   → respondDisallowAndReject
 *
 * Registered in the manifest under `BIND_SCREENING_SERVICE`.  User must pick
 * ScamShield as the "Caller ID and spam app" in system settings for the
 * platform to invoke us.
 */
class ScamShieldCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart
        val callerIdHint = runCatching { callDetails.callerDisplayName }.getOrNull()

        logD(TAG, "onScreenCall: number=$rawNumber, callerId=$callerIdHint")

        scope.launch {
            try {
                val container = ScamShieldApp.container()
                val analyzer = PhoneNumberAnalyzer(applicationContext, container.callRepository)
                val analysis = analyzer.analyze(rawNumber, callerIdHint)

                val response = CallResponse.Builder()
                val action: CallAction

                when {
                    analysis.risk == CallRisk.CRITICAL || analysis.isBlocked -> {
                        response
                            .setDisallowCall(true)
                            .setRejectCall(true)
                            .setSkipCallLog(false)
                            .setSkipNotification(true)
                        action = CallAction.BLOCKED
                        Log.w(TAG, "Auto-blocking call from ${analysis.phoneNumber} (risk=${analysis.risk})")
                    }
                    analysis.risk == CallRisk.HIGH -> {
                        response.setDisallowCall(false)
                        action = CallAction.SCREENED
                        CallOverlayManager.showWarning(applicationContext, analysis)
                        Log.w(TAG, "HIGH-risk call surfaced overlay: ${analysis.phoneNumber}")
                    }
                    analysis.risk == CallRisk.MEDIUM -> {
                        response.setDisallowCall(false)
                        action = CallAction.SCREENED
                        CallOverlayManager.showWarning(applicationContext, analysis)
                        Log.i(TAG, "MEDIUM-risk call surfaced overlay: ${analysis.phoneNumber}")
                    }
                    else -> {
                        response.setDisallowCall(false)
                        action = CallAction.ALLOWED
                    }
                }

                respondToCall(callDetails, response.build())
                container.callRepository.recordCall(analysis, action)
            } catch (t: Throwable) {
                Log.e(TAG, "Screening failed — defaulting to allow", t)
                // Never accidentally block on internal failure
                respondToCall(callDetails, CallResponse.Builder().setDisallowCall(false).build())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    companion object {
        private const val TAG = "CallScreeningSvc"
    }
}
