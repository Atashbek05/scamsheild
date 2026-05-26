package com.example.scamshield.repository

import com.example.scamshield.model.ScamAnalysisResponse
import com.example.scamshield.util.logD
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe in-process rate limiter and result cache for backend calls.
 *
 * Four layers evaluated in order inside [ScamAnalysisRepository.analyzeText]:
 *
 *   1. Result cache (100 LRU entries, 10-min TTL) — returns a cached
 *      [ScamAnalysisResponse] immediately for repeated messages.
 *
 *   2. Dedup window (50 LRU hashes, 5-min TTL) — prevents re-sending a message
 *      whose cached response has expired but was seen recently.  Falls back to
 *      local-only analysis via [ThrottledException].
 *
 *   3. Throttle window (10 requests per 60 s, rolling) — caps sustained backend
 *      traffic.  Excess requests fall back to local analysis.
 *
 *   4. Concurrent-slot guard (max 5 in-flight requests) — bounds parallelism
 *      during sudden spikes.  Requests over the limit are dropped (local only).
 *
 * All [ThrottledException] failures are handled the same as
 * [BackendUnavailableException] in [BaseMessageAnalysisService]: local detectors
 * run and [com.example.scamshield.data.AiConnectionState.Throttled] is published
 * to [com.example.scamshield.data.ThreatStore].
 */
internal object RateLimiter {

    private const val TAG = "RateLimiter"

    // ── TTLs ─────────────────────────────────────────────────────────────
    private const val DEDUP_TTL_MS    = 5L * 60 * 1_000   // 5 min
    private const val CACHE_TTL_MS    = 10L * 60 * 1_000  // 10 min
    private const val THROTTLE_WIN_MS = 60_000L            // 1 min

    // ── Limits ───────────────────────────────────────────────────────────
    private const val MAX_PER_MINUTE    = 10
    private const val MAX_HASH_ENTRIES  = 50
    private const val MAX_CACHE_ENTRIES = 100
    const val MAX_CONCURRENT            = 5

    // ── Result cache (LRU, 100 entries, 10-min TTL) ───────────────────────
    private data class CachedEntry(val response: ScamAnalysisResponse, val timestamp: Long)

    private val resultCache = object : LinkedHashMap<String, CachedEntry>(
        MAX_CACHE_ENTRIES + 1, 0.75f, /* accessOrder = */ true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedEntry>) =
            size > MAX_CACHE_ENTRIES
    }

    // ── Dedup hash set (LRU, 50 entries, 5-min TTL) ───────────────────────
    private val dedupCache = object : LinkedHashMap<String, Long>(
        MAX_HASH_ENTRIES + 1, 0.75f, /* accessOrder = */ true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>) =
            size > MAX_HASH_ENTRIES
    }

    // ── Throttle: timestamps of requests sent in the last 60 s ───────────
    private val windowTimestamps = ArrayDeque<Long>()

    // ── In-flight counter (atomic — no lock needed for inc/dec) ──────────
    private val inFlight = AtomicInteger(0)

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    fun md5(text: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Synchronized
    fun getCached(hash: String): ScamAnalysisResponse? {
        val entry = resultCache[hash] ?: return null
        val age = System.currentTimeMillis() - entry.timestamp
        if (age > CACHE_TTL_MS) {
            resultCache.remove(hash)
            return null
        }
        logD(TAG, "Cache hit | hash=${hash.take(8)} | age=${age / 1_000}s")
        return entry.response
    }

    @Synchronized
    fun putCache(hash: String, response: ScamAnalysisResponse) {
        resultCache[hash] = CachedEntry(response, System.currentTimeMillis())
    }

    @Synchronized
    fun isDuplicate(hash: String): Boolean {
        val now = System.currentTimeMillis()
        val ts  = dedupCache[hash] ?: return false
        if (now - ts > DEDUP_TTL_MS) {
            dedupCache.remove(hash)
            return false
        }
        logD(TAG, "Dedup hit | hash=${hash.take(8)} | age=${(now - ts) / 1_000}s")
        return true
    }

    @Synchronized
    fun recordHash(hash: String) {
        dedupCache[hash] = System.currentTimeMillis()
    }

    @Synchronized
    fun isThrottled(): Boolean {
        val now = System.currentTimeMillis()
        while (windowTimestamps.isNotEmpty() && now - windowTimestamps.first() > THROTTLE_WIN_MS) {
            windowTimestamps.removeFirst()
        }
        if (windowTimestamps.size >= MAX_PER_MINUTE) {
            logD(TAG, "Throttled | ${windowTimestamps.size} req in last 60 s (limit=$MAX_PER_MINUTE)")
            return true
        }
        return false
    }

    @Synchronized
    fun recordRequest() {
        windowTimestamps.addLast(System.currentTimeMillis())
    }

    /**
     * Atomically reserves one in-flight slot. Returns `false` (without modifying
     * the counter) when the limit is already reached — the caller should fall back
     * to local analysis via [ThrottledException].
     */
    fun tryAcquireSlot(): Boolean {
        val prev = inFlight.getAndUpdate { if (it < MAX_CONCURRENT) it + 1 else it }
        if (prev >= MAX_CONCURRENT) {
            logD(TAG, "Slot exhausted | $prev in-flight (limit=$MAX_CONCURRENT)")
            return false
        }
        return true
    }

    fun releaseSlot() {
        inFlight.decrementAndGet()
    }
}
