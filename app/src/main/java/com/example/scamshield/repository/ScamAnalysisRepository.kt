package com.example.scamshield.repository

import android.util.Log
import com.example.scamshield.util.logD
import com.example.scamshield.data.AiConnectionState
import com.example.scamshield.data.ThreatStore
import com.example.scamshield.model.ScamAnalysisRequest
import com.example.scamshield.model.ScamAnalysisResponse
import com.example.scamshield.network.ApiClient
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

/**
 * Single source of truth for scam-analysis data.
 *
 * Wraps every Retrofit call in [Result] so callers never handle raw exceptions —
 * they only pattern-match on success/failure. All request/response logging and
 * error categorisation live here so service-layer call sites stay clean.
 *
 * Request format:  POST /v1/analyze  →  {"message": "<text>"}
 * Response format: {"scam_probability": 0.0–1.0, "label": "scam"|"safe",
 *                   "suspicious_keywords": [...]}
 *
 * Retry policy (evaluated after rate-limiter layers):
 *   IOException (network)  → 3 retries, exponential backoff 1 s / 2 s / 4 s
 *   HTTP 429 (rate limit)  → 1 retry after 30 s wait; sets [AiConnectionState.RateLimited]
 *   HTTP 5xx (server err)  → 2 retries, 2 s / 4 s backoff
 *   HTTP 4xx (client err)  → no retry, immediate fallback
 *
 * Connection state updates:
 *   Success < 5 s          → [AiConnectionState.Online]
 *   Success ≥ 5 s          → [AiConnectionState.Slow]
 *   429 received           → [AiConnectionState.RateLimited]
 *   All network retries ex → [AiConnectionState.Offline]
 *   All server retries ex  → [AiConnectionState.Error]
 */
class BackendUnavailableException(cause: Throwable) : Exception("Backend unavailable", cause)

/** Signals that the request was suppressed by the rate limiter. The caller should
 *  fall back to local-only analysis rather than treating this as a connectivity error. */
class ThrottledException(reason: String) : Exception("Rate limited: $reason")

class ScamAnalysisRepository {

    companion object {
        private const val TAG = "ScamAnalysisRepo"
        private const val LOG_PREVIEW_LEN = 80
        private const val SLOW_THRESHOLD_MS = 5_000L
        private const val RATE_LIMIT_WAIT_MS = 30_000L
        private val NETWORK_BACKOFF_MS = longArrayOf(1_000L, 2_000L, 4_000L)
        private const val MAX_NETWORK_RETRIES = 3
        private const val MAX_SERVER_RETRIES = 2
    }

    private val api = ApiClient.apiService

    /**
     * Sends [message] to POST /v1/analyze and returns a typed [Result].
     *
     * **Must be called from a coroutine running on [kotlinx.coroutines.Dispatchers.IO].**
     *
     * Rate-limiting layers (evaluated in order before the network call):
     *   1. Result cache (10-min TTL, 100 LRU)  → returns cached [ScamAnalysisResponse]
     *   2. Dedup window (5-min TTL, 50 hashes)  → [ThrottledException]
     *   3. Throttle (10 req/min rolling window) → [ThrottledException]
     *   4. Concurrent-slot guard (max 5)         → [ThrottledException]
     */
    suspend fun analyzeText(
        message: String,
        packageName: String,
    ): Result<ScamAnalysisResponse> {

        val hash = RateLimiter.md5(message)

        // ── Layer 1: result cache ─────────────────────────────────────────
        RateLimiter.getCached(hash)?.let { cached ->
            logD(TAG, "↩ Cache | pkg=$packageName | hash=${hash.take(8)}")
            return Result.success(cached)
        }

        // ── Layer 2: dedup (same content, cache already expired) ──────────
        if (RateLimiter.isDuplicate(hash)) {
            logD(TAG, "↩ Dedup | pkg=$packageName | hash=${hash.take(8)}")
            return Result.failure(ThrottledException("duplicate within 5-min window"))
        }

        // ── Layer 3: throttle (>10 req/min) ───────────────────────────────
        if (RateLimiter.isThrottled()) {
            return Result.failure(ThrottledException(">10 req/min"))
        }

        // ── Layer 4: concurrent-slot guard (max 5 in-flight) ─────────────
        if (!RateLimiter.tryAcquireSlot()) {
            return Result.failure(ThrottledException("queue full (>${RateLimiter.MAX_CONCURRENT} concurrent)"))
        }

        val preview  = message.take(LOG_PREVIEW_LEN).replace('\n', ' ')
        val ellipsis = if (message.length > LOG_PREVIEW_LEN) "…" else ""
        logD(TAG, "→ POST /v1/analyze | pkg=$packageName | ${message.length} chars | \"$preview$ellipsis\"")

        RateLimiter.recordHash(hash)
        RateLimiter.recordRequest()

        return try {
            executeWithRetry(message, packageName, hash)
        } finally {
            RateLimiter.releaseSlot()
        }
    }

    /**
     * Executes the API call with the retry policy described in the class-level doc.
     * Updates [ThreatStore] connection state on meaningful transitions.
     */
    private suspend fun executeWithRetry(
        message: String,
        packageName: String,
        hash: String,
    ): Result<ScamAnalysisResponse> {
        var networkAttempt = 0
        var serverAttempt  = 0
        var rateLimitRetried = false

        while (true) {
            val startMs = System.currentTimeMillis()
            try {
                val response = api.analyzeText(ScamAnalysisRequest(message))
                val elapsed  = System.currentTimeMillis() - startMs

                RateLimiter.putCache(hash, response)

                if (elapsed >= SLOW_THRESHOLD_MS) {
                    ThreatStore.setAiConnectionState(AiConnectionState.Slow)
                    logD(TAG, "✓ /v1/analyze (slow ${elapsed}ms) | pkg=$packageName | prob=${"%.3f".format(response.scamProbability)} | label=${response.label}")
                } else {
                    ThreatStore.setAiConnectionState(AiConnectionState.Online)
                    logD(TAG, "✓ /v1/analyze (${elapsed}ms) | pkg=$packageName | prob=${"%.3f".format(response.scamProbability)} | label=${response.label} | keywords(${response.suspiciousKeywords.size})=${response.suspiciousKeywords}")
                }

                return Result.success(response)

            } catch (e: HttpException) {
                val code = e.code()
                when {
                    code == 429 -> {
                        ThreatStore.setAiConnectionState(AiConnectionState.RateLimited)
                        if (rateLimitRetried) {
                            Log.e(TAG, "✗ HTTP 429 — no more retries | pkg=$packageName")
                            FirebaseCrashlytics.getInstance().recordException(e)
                            return Result.failure(e)
                        }
                        rateLimitRetried = true
                        Log.w(TAG, "HTTP 429 — waiting ${RATE_LIMIT_WAIT_MS / 1_000}s before retry | pkg=$packageName")
                        delay(RATE_LIMIT_WAIT_MS)
                    }
                    code in 500..599 -> {
                        serverAttempt++
                        if (serverAttempt >= MAX_SERVER_RETRIES) {
                            Log.e(TAG, "✗ HTTP $code after $serverAttempt retries | pkg=$packageName | ${e.message()}")
                            ThreatStore.setAiConnectionState(AiConnectionState.Error("HTTP $code"))
                            FirebaseCrashlytics.getInstance().recordException(e)
                            return Result.failure(e)
                        }
                        val wait = 2_000L * serverAttempt
                        Log.w(TAG, "HTTP $code — retrying in ${wait}ms (attempt $serverAttempt/$MAX_SERVER_RETRIES) | pkg=$packageName")
                        delay(wait)
                    }
                    else -> {
                        // 4xx (except 429) — caller falls back to local detectors immediately
                        Log.e(TAG, "✗ HTTP $code | pkg=$packageName | ${e.message()}")
                        FirebaseCrashlytics.getInstance().recordException(e)
                        return Result.failure(e)
                    }
                }

            } catch (e: IOException) {
                networkAttempt++
                if (networkAttempt > MAX_NETWORK_RETRIES) {
                    Log.e(TAG, "Backend unavailable after $networkAttempt attempts: ${e.message}")
                    ThreatStore.setAiConnectionState(AiConnectionState.Offline)
                    return Result.failure(BackendUnavailableException(e))
                }
                val wait = NETWORK_BACKOFF_MS.getOrElse(networkAttempt - 1) { 4_000L }
                Log.w(TAG, "Network error (attempt $networkAttempt/$MAX_NETWORK_RETRIES), retry in ${wait}ms: ${e.message}")
                delay(wait)

            } catch (e: Exception) {
                Log.e(TAG, "✗ Unexpected | pkg=$packageName | ${e.javaClass.simpleName}: ${e.message}", e)
                FirebaseCrashlytics.getInstance().recordException(e)
                return Result.failure(e)
            }
        }
    }
}
