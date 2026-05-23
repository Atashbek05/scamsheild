package com.example.scamshield.ui.report

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.scamshield.R
import com.example.scamshield.data.DetectedThreat
import com.example.scamshield.report.PdfReportGenerator
import com.example.scamshield.report.ReportFilter
import kotlinx.coroutines.launch
import java.io.File

// ── Palette (mirrors DashboardScreen) ────────────────────────────────────────

private val CyberBg      = Color(0xFF0A0E1A)
private val CyberSurface = Color(0xFF141828)
private val CyberBorder  = Color(0xFF1E2A45)
private val CyberCyan    = Color(0xFF00D4FF)
private val CyberGreen   = Color(0xFF00FF88)
private val CyberRed     = Color(0xFFFF3B5C)
private val CyberAmber   = Color(0xFFFFB800)
private val CyberMuted   = Color(0xFF6B7A9F)
private val CyberText    = Color(0xFFE8EAF6)

/**
 * Expandable panel that lets the user choose a [ReportFilter], preview the
 * threat count, generate a PDF, and then share it.
 *
 * @param threats       Full unfiltered threat list from ThreatStore.
 * @param totalScanned  Lifetime scanned-message count for the executive summary.
 */
@Composable
fun ReportExportCard(
    threats: List<DetectedThreat>,
    totalScanned: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var expanded       by rememberSaveable { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf(ReportFilter.ALL) }
    var isGenerating   by remember { mutableStateOf(false) }
    var generatedFile  by remember { mutableStateOf<File?>(null) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }

    val filteredCount = selectedFilter.applyTo(threats).size
    val noThreatsMsg  = stringResource(R.string.report_no_threats_filter)
    val shareChooser  = stringResource(R.string.report_share_chooser)
    val emailSubject  = stringResource(R.string.report_email_subject)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CyberSurface,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .animateContentSize(animationSpec = tween(220)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded; errorMessage = null },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    tint   = CyberCyan,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = stringResource(R.string.report_export_title),
                        color      = CyberCyan,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text     = stringResource(R.string.report_export_subtitle),
                        color    = CyberMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(
                    text     = if (expanded) "▲" else "▼",
                    color    = CyberMuted,
                    fontSize = 12.sp,
                )
            }

            // ── Expanded content ──────────────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text       = stringResource(R.string.report_scope),
                        color      = CyberMuted,
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.height(8.dp))

                    ReportFilter.entries.forEach { filter ->
                        FilterOption(
                            filter   = filter,
                            count    = filter.applyTo(threats).size,
                            selected = selectedFilter == filter,
                            onClick  = {
                                selectedFilter = filter
                                generatedFile  = null
                                errorMessage   = null
                            },
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text       = stringResource(R.string.report_threat_summary, filteredCount, threats.size, totalScanned),
                        color      = CyberMuted,
                        fontSize   = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )

                    Spacer(Modifier.height(14.dp))

                    if (errorMessage != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning, null,
                                tint = CyberRed, modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text       = errorMessage!!,
                                color      = CyberRed,
                                fontSize   = 9.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    Row(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                if (filteredCount == 0) {
                                    errorMessage = noThreatsMsg
                                    return@Button
                                }
                                isGenerating  = true
                                errorMessage  = null
                                generatedFile = null
                                scope.launch {
                                    runCatching {
                                        PdfReportGenerator.generate(
                                            context      = context,
                                            threats      = threats,
                                            filter       = selectedFilter,
                                            totalScanned = totalScanned,
                                        )
                                    }.onSuccess { file ->
                                        generatedFile = file
                                    }.onFailure { err ->
                                        errorMessage = context.getString(R.string.report_generate_failed, err.message?.take(60) ?: "")
                                    }
                                    isGenerating = false
                                }
                            },
                            enabled = !isGenerating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor   = CyberBg,
                            ),
                            shape    = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color    = CyberBg,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.report_generating),
                                    fontSize   = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.report_generate_pdf),
                                    fontSize   = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        if (generatedFile != null) {
                            OutlinedButton(
                                onClick = { sharePdf(context, generatedFile!!, shareChooser, emailSubject) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen),
                                shape  = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.report_share),
                                    fontSize   = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    if (generatedFile != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text       = stringResource(R.string.report_saved, generatedFile!!.name),
                            color      = CyberGreen,
                            fontSize   = 9.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

// ── Filter option row ─────────────────────────────────────────────────────────

@Composable
private fun FilterOption(
    filter: ReportFilter,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) CyberCyan else CyberBorder
    val countColor  = when {
        count == 0                        -> CyberMuted
        filter == ReportFilter.HIGH_RISK  -> CyberRed
        else                              -> CyberAmber
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .border(1.5.dp, if (selected) CyberCyan else CyberMuted, RoundedCornerShape(50)),
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.Center)
                        .border(3.dp, CyberCyan, RoundedCornerShape(50)),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = stringResource(filter.labelRes),
                color      = if (selected) CyberText else CyberMuted,
                fontSize   = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text       = stringResource(filter.descriptionRes),
                color      = CyberMuted,
                fontSize   = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        Text(
            text       = stringResource(R.string.report_count_threats, count),
            color      = countColor,
            fontSize   = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

// ── Share helper ──────────────────────────────────────────────────────────────

private fun sharePdf(context: Context, file: File, chooserTitle: String, emailSubject: String) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type    = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
