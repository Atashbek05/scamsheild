package com.example.scamshield.ui.history

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.scamshield.R
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.RiskLevel
import com.example.scamshield.data.db.FeedbackEntity
import com.example.scamshield.data.db.ThreatEntity
import com.example.scamshield.repository.FeedbackRepository
import com.example.scamshield.repository.ThreatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThreatHistoryViewModel(
    private val repo: ThreatRepository,
    private val feedbackRepo: FeedbackRepository,
) : ViewModel() {

    enum class DateRange(val label: String, val sinceMillis: Long?, @StringRes val labelRes: Int) {
        ALL_TIME("All time",   null,                       R.string.date_range_all_time),
        TODAY("Today",         24L * 60 * 60 * 1000,       R.string.date_range_today),
        WEEK("This week",      7L * 24 * 60 * 60 * 1000,  R.string.date_range_week),
        MONTH("This month",    30L * 24 * 60 * 60 * 1000, R.string.date_range_month),
    }

    data class Filters(
        val query: String = "",
        val app: String? = null,
        val minRisk: RiskLevel? = null,
        val range: DateRange = DateRange.ALL_TIME,
    )

    private val _filters = MutableStateFlow(Filters())
    val filters: StateFlow<Filters> = _filters.asStateFlow()

    val packages: StateFlow<List<String>> = repo.observePackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val threats: Flow<PagingData<ThreatEntity>> = repo
        .getThreatsPaged()
        .cachedIn(viewModelScope)

    fun setQuery(value: String)         { _filters.value = _filters.value.copy(query = value) }
    fun setApp(value: String?)          { _filters.value = _filters.value.copy(app = value) }
    fun setMinRisk(value: RiskLevel?)   { _filters.value = _filters.value.copy(minRisk = value) }
    fun setRange(value: DateRange)      { _filters.value = _filters.value.copy(range = value) }
    fun clearFilters()                  { _filters.value = Filters() }

    fun deleteThreat(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun deleteOlderThan(days: Int) {
        viewModelScope.launch { repo.deleteOlderThan(days) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }

    suspend fun getFilteredThreats(): List<ThreatEntity> {
        val f = _filters.value
        val since = f.range.sinceMillis?.let { System.currentTimeMillis() - it }
        return repo.search(
            packageFilter  = f.app,
            minProbability = f.minRisk?.threshold,
            since          = since,
            query          = f.query.ifBlank { null },
        ).first()
    }

    fun submitFeedback(threatId: Long, reason: String, comment: String) {
        viewModelScope.launch {
            feedbackRepo.insert(
                FeedbackEntity(threatId = threatId, reason = reason, comment = comment)
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ThreatHistoryViewModel(
                    ScamShieldApp.container().threatRepository,
                    ScamShieldApp.container().feedbackRepository,
                )
            }
        }
    }
}
