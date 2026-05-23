package com.example.scamshield.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {

    // ── Call history ─────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: CallEntity): Long

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CallEntity>>

    @Query("SELECT COUNT(*) FROM calls")
    fun observeTotal(): Flow<Int>

    @Query("SELECT COUNT(*) FROM calls WHERE probability >= :threshold")
    fun observeRiskyCount(threshold: Float): Flow<Int>

    @Query("SELECT COUNT(*) FROM calls WHERE timestamp >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM calls WHERE timestamp >= :since AND probability >= :threshold")
    fun observeRiskyCountSince(since: Long, threshold: Float): Flow<Int>

    @Query("SELECT COUNT(*) FROM calls WHERE action = 'BLOCKED'")
    fun observeBlockedCount(): Flow<Int>

    @Query("SELECT * FROM calls WHERE phone_number = :number ORDER BY timestamp DESC")
    fun observeByNumber(number: String): Flow<List<CallEntity>>

    @Query("SELECT COUNT(*) FROM calls WHERE phone_number = :number AND timestamp >= :since")
    suspend fun countByNumberSince(number: String, since: Long): Int

    /**
     * Aggregated "top scam callers" view — groups by phone number, ranks by
     * highest probability seen, ties broken by repeat count.
     */
    @Query(
        """
        SELECT phone_number AS phoneNumber,
               MAX(display_name) AS displayName,
               MAX(probability)  AS maxProbability,
               COUNT(*)          AS callCount,
               MAX(timestamp)    AS lastSeen
        FROM calls
        WHERE probability >= :threshold
        GROUP BY phone_number
        ORDER BY maxProbability DESC, callCount DESC
        LIMIT :limit
        """
    )
    fun observeTopScamCallers(threshold: Float, limit: Int): Flow<List<TopScamCallerRow>>

    @Query("DELETE FROM calls")
    suspend fun clearAll()

    @Query("DELETE FROM calls WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ── Blocked numbers ──────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockNumber(entity: BlockedNumberEntity)

    @Query("DELETE FROM blocked_numbers WHERE phone_number = :number")
    suspend fun unblockNumber(number: String)

    @Query("SELECT * FROM blocked_numbers ORDER BY blocked_at DESC")
    fun observeBlocked(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE phone_number = :number)")
    suspend fun isBlocked(number: String): Boolean

    // ── Trusted contacts ─────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun trustNumber(entity: TrustedContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE phone_number = :number")
    suspend fun untrustNumber(number: String)

    @Query("SELECT * FROM trusted_contacts ORDER BY added_at DESC")
    fun observeTrusted(): Flow<List<TrustedContactEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM trusted_contacts WHERE phone_number = :number)")
    suspend fun isTrusted(number: String): Boolean
}

/** Projection row for the "top scam callers" aggregate query. */
data class TopScamCallerRow(
    val phoneNumber: String,
    val displayName: String,
    val maxProbability: Float,
    val callCount: Int,
    val lastSeen: Long,
)
