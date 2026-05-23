package com.example.scamshield.ui.analytics

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamshield.R
import com.example.scamshield.ScamShieldApp
import com.example.scamshield.data.ThreatCategory
import com.example.scamshield.data.ThreatStore
import com.example.scamshield.data.toDetectedThreat
import com.example.scamshield.report.PdfReportGenerator
import com.example.scamshield.report.ReportFilter
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.components.StatTile
import com.example.scamshield.ui.theme.CyberAmber
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen() {
    val vm: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.Factory)
    val analytics by vm.analytics.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Analytics, null, tint = CyberCyan, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.analytics_title), color = CyberTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(stringResource(R.string.analytics_subtitle), color = CyberTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(stringResource(R.string.stat_scanned), analytics.totalScanned.toString(), CyberCyan, Modifier.weight(1f))
            StatTile(stringResource(R.string.stat_threats), analytics.totalThreats.toString(), CyberRed, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(stringResource(R.string.stat_high_risk), analytics.highRiskCount.toString(), CyberRed, Modifier.weight(1f))
            StatTile(stringResource(R.string.stat_detection), "${analytics.detectionRatePct}%", CyberAmber, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))

        SectionCard(stringResource(R.string.analytics_risk_distribution)) { RiskDistributionBar(analytics) }
        Spacer(Modifier.height(14.dp))

        SectionCard(stringResource(R.string.analytics_last_7_days)) {
            BarChart(analytics.daily.map { it.label to it.count }, CyberCyan)
        }
        Spacer(Modifier.height(14.dp))

        SectionCard(stringResource(R.string.analytics_last_6_weeks)) {
            BarChart(analytics.weekly.map { it.label to it.count }, CyberGreen)
        }
        Spacer(Modifier.height(14.dp))

        SectionCard(stringResource(R.string.analytics_by_category)) { CategoryBreakdown(analytics.byCategory) }
        Spacer(Modifier.height(14.dp))

        SectionCard(stringResource(R.string.analytics_by_app)) { AppBreakdown(analytics.byApp) }
        Spacer(Modifier.height(14.dp))

        SectionCard(stringResource(R.string.analytics_keyword_heatmap)) { KeywordHeatmap(analytics.keywords) }
        Spacer(Modifier.height(16.dp))

        PdfExportCard()
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    CyberCard {
        Text(title, color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun RiskDistributionBar(a: AnalyticsViewModel.Analytics) {
    val total = (a.highRiskCount + a.mediumRiskCount + a.lowRiskCount).coerceAtLeast(1).toFloat()
    val high = a.highRiskCount / total
    val med  = a.mediumRiskCount / total
    val low  = a.lowRiskCount / total

    Box(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(CyberBgCard),
    ) {
        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            var x = 0f
            val w = size.width
            drawRect(CyberRed,   topLeft = Offset(x, 0f), size = Size(w * high, size.height)); x += w * high
            drawRect(CyberAmber, topLeft = Offset(x, 0f), size = Size(w * med,  size.height)); x += w * med
            drawRect(CyberGreen, topLeft = Offset(x, 0f), size = Size(w * low,  size.height))
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        LegendBadge(stringResource(R.string.analytics_legend_high), a.highRiskCount, CyberRed)
        LegendBadge(stringResource(R.string.analytics_legend_med),  a.mediumRiskCount, CyberAmber)
        LegendBadge(stringResource(R.string.analytics_legend_low),  a.lowRiskCount, CyberGreen)
    }
}

@Composable
private fun LegendBadge(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.size(4.dp))
        Text("$label $count", color = CyberTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BarChart(bars: List<Pair<String, Int>>, accent: Color) {
    val maxVal = (bars.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1).toFloat()
    val chartHeight = 110.dp
    Row(
        Modifier.fillMaxWidth().height(chartHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { (label, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                val fraction = (value / maxVal).coerceIn(0f, 1f)
                Text(value.toString(), color = CyberTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .fillMaxWidth()
                        .height(chartHeight * fraction.coerceAtLeast(0.05f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent.copy(alpha = 0.8f)),
                )
                Spacer(Modifier.height(4.dp))
                Text(label, color = CyberTextSecondary, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun CategoryBreakdown(buckets: List<AnalyticsViewModel.CategoryBucket>) {
    if (buckets.isEmpty()) {
        Text(stringResource(R.string.analytics_no_category), color = CyberTextSecondary, fontSize = 12.sp); return
    }
    val total = buckets.sumOf { it.count }.coerceAtLeast(1).toFloat()
    buckets.forEach { b ->
        val color = categoryColor(b.category)
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(stringResource(b.category.labelRes), color = CyberTextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("${b.count} · ${(b.count / total * 100).toInt()}%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(CyberBgCard)) {
                Box(Modifier.fillMaxWidth(b.count / total).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
            }
        }
    }
}

@Composable
private fun AppBreakdown(apps: List<AnalyticsViewModel.AppBucket>) {
    if (apps.isEmpty()) {
        Text(stringResource(R.string.analytics_no_apps), color = CyberTextSecondary, fontSize = 12.sp); return
    }
    val max = apps.maxOf { it.count }.coerceAtLeast(1).toFloat()
    apps.forEach { b ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                b.packageName,
                color = CyberTextPrimary, fontSize = 12.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Box(
                Modifier
                    .weight(2f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyberBgCard),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(b.count / max)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberCyan),
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(b.count.toString(), color = CyberTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordHeatmap(keywords: List<AnalyticsViewModel.KeywordHeatItem>) {
    if (keywords.isEmpty()) {
        Text(stringResource(R.string.analytics_no_keywords), color = CyberTextSecondary, fontSize = 12.sp); return
    }
    val max = keywords.maxOf { it.count }.coerceAtLeast(1).toFloat()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keywords.forEach { item ->
            val heat = item.count / max
            val color = blendColor(heat)
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f + heat * 0.55f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("${item.keyword} · ${item.count}", color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun blendColor(heat: Float): Color = when {
    heat > 0.66f -> CyberRed
    heat > 0.33f -> CyberAmber
    else         -> CyberCyan
}

private fun categoryColor(c: ThreatCategory): Color = when (c) {
    ThreatCategory.PHISHING           -> CyberRed
    ThreatCategory.OTP_SCAM           -> CyberRed
    ThreatCategory.FAKE_BANKING       -> CyberRed
    ThreatCategory.SOCIAL_ENGINEERING -> CyberAmber
    ThreatCategory.URGENCY_BAIT       -> CyberAmber
    ThreatCategory.LINK_TRAP          -> CyberCyan
    ThreatCategory.OTHER              -> CyberCyan
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PdfExportCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val totalScanned by ThreatStore.totalScanned.collectAsStateWithLifecycle()
    val repo = remember { ScamShieldApp.container().threatRepository }
    val shareLabel = stringResource(R.string.analytics_share_report)
    val reportFailedTemplate = stringResource(R.string.analytics_report_failed)

    CyberCard(accent = CyberAmber) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PictureAsPdf, null, tint = CyberAmber, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.analytics_pdf_export), color = CyberAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.analytics_pdf_desc), color = CyberTextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportFilter.entries.forEach { f ->
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val snapshot = repo.observeAll().first().map { it.toDetectedThreat() }
                                val file = PdfReportGenerator.generate(context, snapshot, f, totalScanned)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, shareLabel))
                            } catch (e: Exception) {
                                Toast.makeText(context, reportFailedTemplate.format(e.message), Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber, contentColor = CyberBgDeep),
                ) {
                    Text(f.label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
