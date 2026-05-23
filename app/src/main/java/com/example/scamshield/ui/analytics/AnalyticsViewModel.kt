package com.example.scamshield.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.RiskLevel
import com.example.scamshield.data.ThreatCategory
import com.example.scamshield.data.ThreatStore
import com.example.scamshield.data.db.ThreatEntity
import com.example.scamshield.data.splitFromDb
import com.example.scamshield.repository.ThreatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

/**
 * Aggregates the persisted threat history into chart-ready snapshots.
 *
 * All buckets are recomputed when the underlying DAO emits — Room takes care of
 * change notification so we never need a manual refresh.
 */
class AnalyticsViewModel(
    private val repo: ThreatRepository,
) : ViewModel() {

    data class CategoryBucket(val category: ThreatCategory, val count: Int)
    data class RiskBucket(val level: RiskLevel, val count: Int)
    data class TimeBucket(val label: String, val count: Int)
    data class AppBucket(val packageName: String, val count: Int)
    data class KeywordHeatItem(val keyword: String, val count: Int)

    data class Analytics(
        val totalScanned: Int = 0,
        val totalThreats: Int = 0,
        val highRiskCount: Int = 0,
        val mediumRiskCount: Int = 0,
        val lowRiskCount: Int = 0,
        val detectionRatePct: Int = 0,
        val byCategory: List<CategoryBucket> = emptyList(),
        val byRisk: List<RiskBucket> = emptyList(),
        val daily: List<TimeBucket> = emptyList(),  // last 7 days
        val weekly: List<TimeBucket> = emptyList(), // last 6 weeks
        val byApp: List<AppBucket> = emptyList(),
        val keywords: List<KeywordHeatItem> = emptyList(),
    )

    val analytics: StateFlow<Analytics> = combine(
        repo.observeAll(),
        ThreatStore.totalScanned,
    ) { entities, scanned -> compute(entities, scanned) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Analytics())

    private fun compute(list: List<ThreatEntity>, scanned: Int): Analytics {
        if (list.isEmpty()) {
            return Analytics(totalScanned = scanned)
        }

        val byCategory = list.groupingBy { ThreatCategory.fromString(it.category) }
            .eachCount()
            .map { (cat, c) -> CategoryBucket(cat, c) }
            .sortedByDescending { it.count }

        val byRisk = RiskLevel.entries.map { lvl ->
            RiskBucket(lvl, list.count { it.riskLevel == lvl.name })
        }

        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        val daily = (6 downTo 0).map { offset ->
            val start = startOfDay(now - offset * dayMs)
            val end   = start + dayMs
            TimeBucket(label = shortDay(start), count = list.count { it.timestamp in start until end })
        }
        val weekly = (5 downTo 0).map { offset ->
            val start = now - (offset + 1) * 7 * dayMs
            val end   = now - offset * 7 * dayMs
            TimeBucket(label = "W-${offset}", count = list.count { it.timestamp in start until end })
        }

        val byApp = list.groupingBy { it.appLabel.ifBlank { it.sourcePackage } }
            .eachCount()
            .map { (pkg, c) -> AppBucket(pkg, c) }
            .sortedByDescending { it.count }
            .take(6)

        val keywords = list.flatMap { it.keywords.splitFromDb() + it.urgencyPatterns.splitFromDb() + it.socialIndicators.splitFromDb() }
            .filter { it.isNotBlank() }
            .groupingBy { it.lowercase() }
            .eachCount()
            .map { (k, c) -> KeywordHeatItem(k, c) }
            .sortedByDescending { it.count }
            .take(20)

        val highRisk = list.count { it.probability >= RiskLevel.HIGH.threshold }
        val medRisk = list.count { it.probability in RiskLevel.MEDIUM.threshold..(RiskLevel.HIGH.threshold - 0.001f) }
        val lowRisk = list.count { it.probability < RiskLevel.MEDIUM.threshold }

        val detRate = if (scanned > 0) (list.size * 100 / scanned).coerceAtMost(100) else 0

        return Analytics(
            totalScanned = scanned,
            totalThreats = list.size,
            highRiskCount = highRisk,
            mediumRiskCount = medRisk,
            lowRiskCount = lowRisk,
            detectionRatePct = detRate,
            byCategory = byCategory,
            byRisk = byRisk,
            daily = daily,
            weekly = weekly,
            byApp = byApp,
            keywords = keywords,
        )
    }

    private fun startOfDay(time: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = time
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun shortDay(time: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        return when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Sun"; Calendar.MONDAY -> "Mon"; Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"; Calendar.THURSDAY -> "Thu"; Calendar.FRIDAY -> "Fri"
            else -> "Sat"
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AnalyticsViewModel(ScamShieldApp.container().threatRepository)
            }
        }
    }
}
