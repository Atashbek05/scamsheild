package com.example.scamshield.repository

import android.content.Context
import com.example.scamshield.data.call.CallAction
import com.example.scamshield.data.call.CallAnalysis
import com.example.scamshield.data.call.CallEvent
import com.example.scamshield.data.call.CallFlag
import com.example.scamshield.data.call.CallRisk
import com.example.scamshield.data.call.CallStore
import com.example.scamshield.data.db.BlockedNumberEntity
import com.example.scamshield.data.db.CallDao
import com.example.scamshield.data.db.CallEntity
import com.example.scamshield.data.db.ScamShieldDatabase
import com.example.scamshield.data.db.TopScamCallerRow
import com.example.scamshield.data.db.TrustedContactEntity
import kotlinx.coroutines.flow.Flow

/**
 * Durable + live store for everything call-protection-related.  Mirrors the
 * pattern set by [ThreatRepository] — DAO behind a thin wrapper, with a
 * companion-object singleton.
 */
class CallRepository(private val dao: CallDao) {

    // ── Call events ──────────────────────────────────────────────────────────

    suspend fun recordCall(
        analysis: CallAnalysis,
        action: CallAction,
        durationSeconds: Int = 0,
    ): CallEvent {
        val timestamp = System.currentTimeMillis()
        val entity = CallEntity(
            phoneNumber     = analysis.phoneNumber,
            displayName     = analysis.displayName,
            probability     = analysis.probability,
            risk            = analysis.risk.name,
            flags           = analysis.flags.joinToString("|") { it.name },
            explanation     = analysis.explanation,
            action          = action.name,
            durationSeconds = durationSeconds,
            timestamp       = timestamp,
        )
        val id = dao.insert(entity)
        val event = CallEvent(
            id              = id,
            phoneNumber     = analysis.phoneNumber,
            displayName     = analysis.displayName,
            probability     = analysis.probability,
            risk            = analysis.risk,
            flags           = analysis.flags,
            explanation     = analysis.explanation,
            action          = action,
            durationSeconds = durationSeconds,
            timestamp       = timestamp,
        )
        CallStore.addEvent(event)
        return event
    }

    fun observeRecent(limit: Int = 20): Flow<List<CallEntity>> = dao.observeRecent(limit)

    fun observeAll(): Flow<List<CallEntity>> = dao.observeAll()

    fun observeTotal(): Flow<Int> = dao.observeTotal()

    fun observeRiskyCount(): Flow<Int> = dao.observeRiskyCount(CallRisk.MEDIUM.threshold)

    fun observeBlockedCount(): Flow<Int> = dao.observeBlockedCount()

    fun observeCountSince(since: Long): Flow<Int> = dao.observeCountSince(since)

    fun observeRiskyCountSince(since: Long): Flow<Int> =
        dao.observeRiskyCountSince(since, CallRisk.MEDIUM.threshold)

    fun observeTopScamCallers(limit: Int = 5): Flow<List<TopScamCallerRow>> =
        dao.observeTopScamCallers(CallRisk.MEDIUM.threshold, limit)

    fun observeByNumber(number: String): Flow<List<CallEntity>> = dao.observeByNumber(number)

    suspend fun countByNumberSince(number: String, since: Long): Int =
        dao.countByNumberSince(number, since)

    suspend fun clearHistory() = dao.clearAll()

    suspend fun deleteCall(id: Long) = dao.deleteById(id)

    // ── Blocked numbers ──────────────────────────────────────────────────────

    suspend fun block(number: String, reason: String) {
        dao.blockNumber(
            BlockedNumberEntity(
                phoneNumber = number,
                reason      = reason,
                blockedAt   = System.currentTimeMillis(),
            )
        )
    }

    suspend fun unblock(number: String) = dao.unblockNumber(number)

    suspend fun isBlocked(number: String): Boolean = dao.isBlocked(number)

    fun observeBlocked(): Flow<List<BlockedNumberEntity>> = dao.observeBlocked()

    // ── Trusted contacts ─────────────────────────────────────────────────────

    suspend fun trust(number: String, name: String) {
        dao.trustNumber(
            TrustedContactEntity(
                phoneNumber = number,
                name        = name,
                addedAt     = System.currentTimeMillis(),
            )
        )
    }

    suspend fun untrust(number: String) = dao.untrustNumber(number)

    suspend fun isTrusted(number: String): Boolean = dao.isTrusted(number)

    fun observeTrusted(): Flow<List<TrustedContactEntity>> = dao.observeTrusted()

    /** Flag mapping helper for UI — converts the stored "FLAG_A|FLAG_B" back to a list. */
    fun parseFlags(stored: String): List<CallFlag> =
        stored.split('|')
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { CallFlag.valueOf(it) }.getOrNull() }

    companion object {
        @Volatile
        private var INSTANCE: CallRepository? = null

        fun get(context: Context): CallRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallRepository(
                    ScamShieldDatabase.get(context).callDao(),
                ).also { INSTANCE = it }
            }
    }
}
