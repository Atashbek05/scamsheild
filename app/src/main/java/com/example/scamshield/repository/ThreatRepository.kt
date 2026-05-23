package com.example.scamshield.repository

import android.content.Context
import com.example.scamshield.data.DetectedThreat
import com.example.scamshield.data.RiskLevel
import com.example.scamshield.data.ThreatCategory
import com.example.scamshield.data.db.ScamShieldDatabase
import com.example.scamshield.data.db.ThreatDao
import com.example.scamshield.data.db.ThreatEntity
import com.example.scamshield.data.toEntity
import kotlinx.coroutines.flow.Flow

/**
 * Durable backing store for detected threats — wraps [ThreatDao] and converts
 * to/from the runtime data classes so callers never see Room types.
 *
 * The in-memory [com.example.scamshield.data.ThreatStore] remains the live cache
 * used by the dashboard for instant updates; this repository feeds the persisted
 * history, analytics, and PDF export flows.
 */
class ThreatRepository(private val dao: ThreatDao) {

    suspend fun insert(
        threat: DetectedThreat,
        appLabel: String,
        category: ThreatCategory,
        backendLabel: String,
    ): Long = dao.insert(threat.toEntity(appLabel, category, backendLabel))

    fun observeAll(): Flow<List<ThreatEntity>> = dao.observeAll()

    fun observeRecent(limit: Int = 20): Flow<List<ThreatEntity>> = dao.observeRecent(limit)

    fun observeCount(): Flow<Int> = dao.observeCount()

    fun observeHighRiskCount(): Flow<Int> = dao.observeHighRiskCount(RiskLevel.HIGH.threshold)

    fun observeCountSince(since: Long): Flow<Int> = dao.observeCountSince(since)

    fun observePackages(): Flow<List<String>> = dao.observePackages()

    fun search(
        packageFilter: String?,
        minProbability: Float?,
        since: Long?,
        query: String?,
    ): Flow<List<ThreatEntity>> = dao.search(packageFilter, minProbability, since, query)

    suspend fun clearAll() = dao.deleteAll()

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun getById(id: Long): ThreatEntity? = dao.getById(id)

    companion object {
        @Volatile
        private var INSTANCE: ThreatRepository? = null

        fun get(context: Context): ThreatRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThreatRepository(
                    ScamShieldDatabase.get(context).threatDao()
                ).also { INSTANCE = it }
            }
    }
}
