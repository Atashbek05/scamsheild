package com.example.scamshield.ui.report

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
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
import com.example.scamshield.ui.theme.CyberAmber
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBorder
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import kotlinx.coroutines.launch
import java.io.File

/**
 * Expandable panel for PDF generation and sharing.
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

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberBgCard)
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            .animateContentSize(animationSpec = tween(220))
            .padding(16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded; errorMessage = null },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.PictureAsPdf, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.report_export_title),
                    color      = CyberCyan,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.report_export_subtitle),
                    color    = CyberTextMuted,
                    fontSize = 10.sp,
                )
            }
            Text(if (expanded) "▲" else "▼", color = CyberTextMuted, fontSize = 11.sp)
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.report_scope),
                    color         = CyberTextMuted,
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 1.sp,
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
                    stringResource(R.string.report_threat_summary, filteredCount, threats.size, totalScanned),
                    color    = CyberTextMuted,
                    fontSize = 9.sp,
                )
                Spacer(Modifier.height(14.dp))

                if (errorMessage != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = CyberRed, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(errorMessage!!, color = CyberRed, fontSize = 9.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (filteredCount == 0) { errorMessage = noThreatsMsg; return@Button }
                            isGenerating = true; errorMessage = null; generatedFile = null
                            scope.launch {
                                runCatching {
                                    PdfReportGenerator.generate(context, threats, selectedFilter, totalScanned)
                                }.onSuccess { generatedFile = it }
                                 .onFailure { errorMessage = context.getString(R.string.report_generate_failed, it.message?.take(60) ?: "") }
                                isGenerating = false
                            }
                        },
                        enabled  = !isGenerating,
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBgDeep),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CyberBgDeep, strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Rounded.PictureAsPdf, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            stringResource(if (isGenerating) R.string.report_generating else R.string.report_generate_pdf),
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    if (generatedFile != null) {
                        OutlinedButton(
                            onClick  = { sharePdf(context, generatedFile!!, shareChooser, emailSubject) },
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f)),
                            shape    = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Share, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.report_share), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (generatedFile != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.report_saved, generatedFile!!.name), color = CyberGreen, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun FilterOption(filter: ReportFilter, count: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) CyberCyan.copy(alpha = 0.08f) else CyberBgDeep)
            .border(1.dp, if (selected) CyberCyan.copy(alpha = 0.4f) else CyberBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(14.dp)
                .border(1.5.dp, if (selected) CyberCyan else CyberTextMuted, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(7.dp).background(CyberCyan, RoundedCornerShape(4.dp)))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(filter.labelRes),
                color      = if (selected) CyberTextPrimary else CyberTextMuted,
                fontSize   = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(stringResource(filter.descriptionRes), color = CyberTextMuted, fontSize = 9.sp)
        }
        Text(
            stringResource(R.string.report_count_threats, count),
            color      = if (count == 0) CyberTextMuted else CyberAmber,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun sharePdf(context: Context, file: File, chooserTitle: String, emailSubject: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
