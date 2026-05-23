package com.example.scamshield.ui.history

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scamshield.R
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.RiskLevel
import com.example.scamshield.data.db.ThreatEntity
import com.example.scamshield.repository.ThreatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ThreatHistoryViewModel(
    private val repo: ThreatRepository,
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

    val threats: StateFlow<List<ThreatEntity>> = _filters
        .flatMapLatest { f ->
            val since = f.range.sinceMillis?.let { System.currentTimeMillis() - it }
            repo.search(
                packageFilter = f.app,
                minProbability = f.minRisk?.threshold,
                since = since,
                query = f.query.takeIf { it.isNotBlank() },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String)         { _filters.value = _filters.value.copy(query = value) }
    fun setApp(value: String?)          { _filters.value = _filters.value.copy(app = value) }
    fun setMinRisk(value: RiskLevel?)   { _filters.value = _filters.value.copy(minRisk = value) }
    fun setRange(value: DateRange)      { _filters.value = _filters.value.copy(range = value) }
    fun clearFilters()                  { _filters.value = Filters() }

    fun deleteThreat(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ThreatHistoryViewModel(ScamShieldApp.container().threatRepository)
            }
        }
    }
}
