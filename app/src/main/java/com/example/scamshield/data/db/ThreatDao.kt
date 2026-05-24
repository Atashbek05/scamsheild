package com.example.scamshield.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(threat: ThreatEntity): Long

    @Query("SELECT * FROM threats ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ThreatEntity>>

    @Query("SELECT * FROM threats ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ThreatEntity>>

    @Query("SELECT * FROM threats ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ThreatEntity>

    @Query("SELECT * FROM threats WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ThreatEntity?

    @Query("SELECT * FROM threats ORDER BY timestamp DESC")
    fun getAllThreatsPaged(): PagingSource<Int, ThreatEntity>

    @Query("DELETE FROM threats WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM threats")
    suspend fun deleteAll()

    @Query("DELETE FROM threats WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM threats")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM threats WHERE probability >= :threshold")
    fun observeHighRiskCount(threshold: Float): Flow<Int>

    @Query("SELECT COUNT(*) FROM threats WHERE timestamp >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Query("SELECT DISTINCT source_package FROM threats")
    fun observePackages(): Flow<List<String>>

    @Query(
        """
        SELECT * FROM threats
        WHERE (:packageFilter IS NULL OR source_package = :packageFilter)
          AND (:minProbability IS NULL OR probability >= :minProbability)
          AND (:since IS NULL OR timestamp >= :since)
          AND (:query IS NULL OR message_preview LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
        """
    )
    fun search(
        packageFilter: String?,
        minProbability: Float?,
        since: Long?,
        query: String?,
    ): Flow<List<ThreatEntity>>
}
