package com.example.scamshield.network

import android.util.Log
import com.example.scamshield.util.logD
import com.example.scamshield.BuildConfig
import com.example.scamshield.data.AiConnectionState
import com.example.scamshield.data.AiTestResult
import com.example.scamshield.data.AiTestState
import com.example.scamshield.data.LogLevel
import com.example.scamshield.data.ThreatStore
import com.example.scamshield.repository.ScamAnalysisRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object AiConnectionMonitor {

    private const val TAG = "AiConnMonitor"
    private const val PING_INTERVAL_MS = 30_000L
    private const val SLOW_THRESHOLD_MS = 5_000L
    private val BASE_URL = BuildConfig.BACKEND_URL
    private const val SAMPLE_SCAM =
        "Congratulations! You have been selected for a \$10,000 prize. " +
            "Click the link to claim your reward immediately. " +
            "Provide your bank account details to receive the transfer."

    // Lightweight client just for health pings — shorter timeouts than the main client
    private val pingClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val repository = ScamAnalysisRepository()
    private var monitorJob: Job? = null

    /** Start periodic health checks. Call from a lifecycle-aware scope. */
    fun start(scope: CoroutineScope) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                checkConnection()
                delay(PING_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private suspend fun checkConnection() {
        ThreatStore.setAiConnectionState(AiConnectionState.Connecting)
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(BASE_URL).head().build()
                val startMs = System.currentTimeMillis()
                pingClient.newCall(request).execute().use { response ->
                    val elapsed = System.currentTimeMillis() - startMs
                    val msg = "Ping: HTTP ${response.code} in ${elapsed}ms"
                    logD(TAG, msg)
                    ThreatStore.addLog(LogLevel.DEBUG, TAG, msg)

                    if (elapsed >= SLOW_THRESHOLD_MS) {
                        ThreatStore.setAiConnectionState(AiConnectionState.Slow)
                        ThreatStore.addLog(LogLevel.INFO, TAG, "Backend slow (${elapsed}ms) — cold start")
                    } else {
                        ThreatStore.setAiConnectionState(AiConnectionState.Online)
                        ThreatStore.addLog(LogLevel.INFO, TAG, "Backend online")
                    }
                }
            } catch (e: IOException) {
                val msg = "${e.javaClass.simpleName}: ${e.message}"
                Log.e(TAG, "Offline — $msg")
                ThreatStore.setAiConnectionState(AiConnectionState.Offline)
                ThreatStore.addLog(LogLevel.ERROR, TAG, "Offline — $msg")
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                Log.e(TAG, "Error — $msg")
                ThreatStore.setAiConnectionState(AiConnectionState.Error(msg))
                ThreatStore.addLog(LogLevel.ERROR, TAG, "Error — $msg")
            }
        }
    }

    /**
     * Send a sample scam payload and surface the result in ThreatStore.aiTestState.
     * Connection state is driven by [ScamAnalysisRepository.analyzeText] — this function
     * only updates [AiTestState].
     */
    fun runTest(scope: CoroutineScope) {
        scope.launch {
            ThreatStore.setAiConnectionState(AiConnectionState.Connecting)
            ThreatStore.setAiTestState(AiTestState.Loading)
            ThreatStore.addLog(LogLevel.INFO, TAG, "Test: sending sample scam text…")
            withContext(Dispatchers.IO) {
                repository.analyzeText(SAMPLE_SCAM, "dashboard_test").fold(
                    onSuccess = { response ->
                        val msg = "Test OK: prob=${"%.1f".format(response.scamProbability * 100)}% " +
                            "label=${response.label} " +
                            "kw=${response.suspiciousKeywords.take(3)}"
                        logD(TAG, msg)
                        ThreatStore.addLog(LogLevel.INFO, TAG, msg)
                        // Connection state already updated by executeWithRetry in the repository
                        ThreatStore.setAiTestState(
                            AiTestState.Success(
                                AiTestResult(
                                    scamProbability = response.scamProbability,
                                    label           = response.label,
                                    keywords        = response.suspiciousKeywords,
                                ),
                            ),
                        )
                    },
                    onFailure = { e ->
                        val msg = e.message ?: "Test failed"
                        Log.e(TAG, "Test failed: $msg")
                        ThreatStore.addLog(LogLevel.ERROR, TAG, "Test failed: $msg")
                        // Connection state already updated by executeWithRetry in the repository
                        ThreatStore.setAiTestState(AiTestState.Error(msg))
                    },
                )
            }
        }
    }
}
