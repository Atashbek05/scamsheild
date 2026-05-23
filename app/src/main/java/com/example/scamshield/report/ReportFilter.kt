package com.example.scamshield.report

import androidx.annotation.StringRes
import com.example.scamshield.R
import com.example.scamshield.data.DetectedThreat

/**
 * Determines which threats appear in an exported PDF report.
 *
 * Applied via [applyTo] so the PDF generator stays filter-agnostic.
 */
enum class ReportFilter(
    val label: String,
    val description: String,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
) {

    /** Every recorded threat, newest first. */
    ALL(
        "All Threats",
        "Export complete threat history",
        R.string.report_filter_all,
        R.string.report_filter_all_desc,
    ),

    /** Only threats whose scam probability exceeds the high-risk threshold. */
    HIGH_RISK(
        "High Risk Only",
        "Threats with probability > 70%",
        R.string.report_filter_high_risk,
        R.string.report_filter_high_risk_desc,
    ),

    /** Threats captured within the last 24 hours. */
    LAST_24H(
        "Last 24 Hours",
        "Recent threats from today",
        R.string.report_filter_24h,
        R.string.report_filter_24h_desc,
    );

    companion object {
        private const val HIGH_RISK_THRESHOLD = 0.70f
        private const val MS_IN_24H = 24L * 60 * 60 * 1_000
    }

    /** Returns the subset of [threats] that this filter accepts, newest first. */
    fun applyTo(threats: List<DetectedThreat>): List<DetectedThreat> = when (this) {
        ALL      -> threats
        HIGH_RISK -> threats.filter { it.probability >= HIGH_RISK_THRESHOLD }
        LAST_24H  -> {
            val cutoff = System.currentTimeMillis() - MS_IN_24H
            threats.filter { it.timestamp >= cutoff }
        }
    }
}
