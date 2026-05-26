package com.example.scamshield.ui.history

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.scamshield.R
import com.example.scamshield.data.RiskLevel
import com.example.scamshield.data.db.ThreatEntity
import com.example.scamshield.data.riskLevelEnum
import com.example.scamshield.data.splitFromDb
import com.example.scamshield.data.toDetectedThreat
import com.example.scamshield.report.DataExportManager
import com.example.scamshield.report.PdfReportGenerator
import com.example.scamshield.report.ReportFilter
import kotlinx.coroutines.launch
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.components.NeonChip
import com.example.scamshield.ui.components.RiskBar
import com.example.scamshield.ui.components.riskColor
import com.example.scamshield.ui.theme.CyberAmber
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Fmt = SimpleDateFormat("MMM dd · HH:mm", Locale.US)

private enum class FeedbackReason(@StringRes val labelRes: Int) {
    KNOWN_COMPANY(R.string.feedback_reason_known_company),
    FRIEND_RELATIVE(R.string.feedback_reason_friend),
    OTHER(R.string.feedback_reason_other),
}

private enum class ExportFormat { PDF, CSV, JSON }

@Composable
fun ThreatHistoryScreen() {
    val vm       = viewModel<ThreatHistoryViewModel>(factory = ThreatHistoryViewModel.Factory)
    val filters  by vm.filters.collectAsStateWithLifecycle()
    val threats  = vm.threats.collectAsLazyPagingItems()
    val packages by vm.packages.collectAsStateWithLifecycle()

    var showExportSheet by remember { mutableStateOf(false) }
    var isExporting     by remember { mutableStateOf(false) }
    val scope   = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep),
        contentPadding    = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                count    = threats.itemCount,
                onClear  = { vm.clearAll() },
                onExport = { showExportSheet = true },
            )
        }

        item {
            OutlinedTextField(
                value          = filters.query,
                onValueChange  = { vm.setQuery(it) },
                placeholder    = { Text(stringResource(R.string.history_search_placeholder), color = CyberTextMuted, fontSize = 13.sp) },
                leadingIcon    = { Icon(Icons.Rounded.Search, null, tint = CyberCyan, modifier = Modifier.size(20.dp)) },
                trailingIcon   = {
                    if (filters.query.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close, null,
                            tint     = CyberTextSecondary,
                            modifier = Modifier.size(18.dp).clickable { vm.setQuery("") },
                        )
                    }
                },
                singleLine     = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors         = searchFieldColors(),
                shape          = RoundedCornerShape(14.dp),
                modifier       = Modifier.fillMaxWidth(),
            )
        }

        item { FilterRow(filters, packages, vm) }

        when {
            threats.loadState.refresh is LoadState.Loading -> {
                items(5) { ShimmerThreatCard() }
            }
            threats.loadState.refresh is LoadState.Error -> {
                item { Text(stringResource(R.string.history_empty), color = CyberRed, fontSize = 12.sp) }
            }
            threats.itemCount == 0 -> {
                item { EmptyState() }
            }
            else -> {
                items(count = threats.itemCount) { index ->
                    val threat = threats[index] ?: return@items
                    ThreatItem(
                        threat     = threat,
                        onDelete   = { vm.deleteThreat(threat.id) },
                        onFeedback = { reason, comment -> vm.submitFeedback(threat.id, reason, comment) },
                    )
                }
            }
        }
    }

    if (showExportSheet) {
        ExportBottomSheet(
            isExporting = isExporting,
            onDismiss   = { if (!isExporting) showExportSheet = false },
            onFormat    = { format ->
                isExporting = true
                scope.launch {
                    runCatching {
                        val data = vm.getFilteredThreats()
                        when (format) {
                            ExportFormat.PDF -> {
                                val file = PdfReportGenerator.generate(
                                    context,
                                    data.map { it.toDetectedThreat() },
                                    ReportFilter.ALL,
                                    data.size,
                                )
                                val uri = FileProvider.getUriForFile(
                                    context, "${context.packageName}.fileprovider", file,
                                )
                                buildShareIntent("application/pdf", uri)
                            }
                            ExportFormat.CSV  -> buildShareIntent(
                                "text/csv",
                                DataExportManager.exportToCsv(context, data),
                            )
                            ExportFormat.JSON -> buildShareIntent(
                                "application/json",
                                DataExportManager.exportToJson(context, data),
                            )
                        }
                    }.onSuccess { intent ->
                        isExporting = false
                        showExportSheet = false
                        context.startActivity(
                            Intent.createChooser(intent, context.getString(R.string.export_share_chooser))
                        )
                    }.onFailure { e ->
                        isExporting = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.export_error, e.message?.take(60) ?: ""),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            },
        )
    }
}

@Composable
private fun ScreenHeader(count: Int, onClear: () -> Unit, onExport: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.history_title),
                color         = CyberTextPrimary,
                fontWeight    = FontWeight.Bold,
                fontSize      = 20.sp,
                letterSpacing = 0.sp,
            )
            Text(
                stringResource(R.string.history_record_count, count),
                color    = CyberTextSecondary,
                fontSize = 12.sp,
            )
        }
        TextButton(onClick = onExport) {
            Icon(Icons.Rounded.Share, null, tint = CyberCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.padding(3.dp))
            Text(stringResource(R.string.export_button), color = CyberCyan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        TextButton(onClick = onClear) {
            Icon(Icons.Rounded.DeleteForever, null, tint = CyberRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.padding(3.dp))
            Text(stringResource(R.string.history_clear), color = CyberRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FilterRow(
    filters: ThreatHistoryViewModel.Filters,
    packages: List<String>,
    vm: ThreatHistoryViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ThreatHistoryViewModel.DateRange.entries.forEach { range ->
                FilterChip(
                    label    = stringResource(range.labelRes),
                    selected = filters.range == range,
                    onClick  = { vm.setRange(range) },
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(stringResource(R.string.history_filter_all_risk), filters.minRisk == null) { vm.setMinRisk(null) }
            listOf(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.CRITICAL).forEach { lvl ->
                FilterChip(
                    stringResource(R.string.history_filter_min_risk, stringResource(lvl.labelRes)),
                    filters.minRisk == lvl,
                ) { vm.setMinRisk(lvl) }
            }
        }
        if (packages.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(stringResource(R.string.history_filter_all_apps), filters.app == null) { vm.setApp(null) }
                packages.forEach { pkg ->
                    FilterChip(pkg.substringAfterLast('.'), filters.app == pkg) { vm.setApp(pkg) }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) CyberCyan.copy(alpha = 0.15f) else CyberBgCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            color      = if (selected) CyberCyan else CyberTextSecondary,
            fontSize   = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreatItem(
    threat: ThreatEntity,
    onDelete: () -> Unit,
    onFeedback: (reason: String, comment: String) -> Unit,
) {
    var expanded          by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var selectedReason    by remember { mutableStateOf<FeedbackReason?>(null) }
    var commentText       by remember { mutableStateOf("") }
    val context           = LocalContext.current
    val risk              = threat.riskLevelEnum()
    val color             = riskColor(risk)

    CyberCard(accent = color) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Text(
                        "${(threat.probability * 100).toInt()}%",
                        color      = color,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 11.sp,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        threat.appLabel.ifBlank { threat.sourcePackage.substringAfterLast('.') }.uppercase(),
                        color      = CyberTextPrimary,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    Text(Fmt.format(Date(threat.timestamp)), color = CyberTextSecondary, fontSize = 10.sp)
                }
                Text(
                    stringResource(risk.labelRes).uppercase(),
                    color      = color,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 11.sp,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null,
                    tint     = CyberTextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                threat.messagePreview,
                color    = CyberTextPrimary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = if (expanded) 8 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            AnimatedVisibility(expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RiskBar(probability = threat.probability, riskLevel = risk, showLabel = true)
                    Spacer(Modifier.height(12.dp))

                    if (threat.keywords.isNotBlank()) {
                        SectionLabel(stringResource(R.string.history_section_keywords))
                        ChipRow(threat.keywords.splitFromDb(), riskColor(risk))
                    }
                    if (threat.phishingLinks.isNotBlank()) {
                        SectionLabel(stringResource(R.string.history_section_links))
                        ChipRow(threat.phishingLinks.splitFromDb(), CyberRed)
                    }
                    if (threat.urgencyPatterns.isNotBlank()) {
                        SectionLabel(stringResource(R.string.history_section_urgency))
                        ChipRow(threat.urgencyPatterns.splitFromDb(), CyberAmber)
                    }
                    if (threat.socialIndicators.isNotBlank()) {
                        SectionLabel(stringResource(R.string.history_section_social))
                        ChipRow(threat.socialIndicators.splitFromDb(), CyberAmber)
                    }
                    if (threat.explanation.isNotBlank()) {
                        SectionLabel(stringResource(R.string.history_section_ai))
                        Text(threat.explanation, color = CyberTextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(onClick = { showFeedbackSheet = true }) {
                            Icon(Icons.Rounded.ThumbDown, null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(
                                stringResource(R.string.feedback_button),
                                color      = CyberCyan,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 12.sp,
                            )
                        }
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Rounded.DeleteForever, null, tint = CyberRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(stringResource(R.string.history_delete), color = CyberRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showFeedbackSheet) {
        FeedbackBottomSheet(
            selectedReason  = selectedReason,
            onReasonSelect  = { selectedReason = it },
            comment         = commentText,
            onCommentChange = { commentText = it },
            onDismiss       = {
                showFeedbackSheet = false
                selectedReason    = null
                commentText       = ""
            },
            onSubmit        = { reason, comment ->
                onFeedback(reason, comment)
                showFeedbackSheet = false
                selectedReason    = null
                commentText       = ""
                Toast.makeText(context, context.getString(R.string.feedback_toast), Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackBottomSheet(
    selectedReason: FeedbackReason?,
    onReasonSelect: (FeedbackReason) -> Unit,
    comment: String,
    onCommentChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, comment: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = CyberBgCard,
        dragHandle       = {
            Box(
                Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CyberTextMuted.copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                stringResource(R.string.feedback_title),
                color      = CyberTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
            )
            Spacer(Modifier.height(16.dp))

            FeedbackReason.entries.forEach { reason ->
                val selected = selectedReason == reason
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) CyberCyan.copy(alpha = 0.08f) else Color.Transparent)
                        .clickable { onReasonSelect(reason) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (selected) CyberCyan else Color.Transparent)
                            .border(1.5.dp, if (selected) CyberCyan else CyberTextMuted, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .background(CyberBgCard, CircleShape),
                            )
                        }
                    }
                    Text(
                        stringResource(reason.labelRes),
                        color      = if (selected) CyberTextPrimary else CyberTextSecondary,
                        fontSize   = 14.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }

            AnimatedVisibility(selectedReason == FeedbackReason.OTHER) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value         = comment,
                        onValueChange = onCommentChange,
                        placeholder   = {
                            Text(stringResource(R.string.feedback_comment_hint), color = CyberTextMuted, fontSize = 13.sp)
                        },
                        colors        = searchFieldColors(),
                        shape         = RoundedCornerShape(12.dp),
                        modifier      = Modifier.fillMaxWidth(),
                        minLines      = 2,
                        maxLines      = 4,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.feedback_cancel), color = CyberTextSecondary, fontSize = 14.sp)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick  = {
                        selectedReason?.let { reason ->
                            onSubmit(reason.name, if (reason == FeedbackReason.OTHER) comment else "")
                        }
                    },
                    enabled  = selectedReason != null,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = CyberCyan,
                        contentColor           = CyberBgDeep,
                        disabledContainerColor = CyberCyan.copy(alpha = 0.25f),
                        disabledContentColor   = CyberBgDeep.copy(alpha = 0.5f),
                    ),
                ) {
                    Text(stringResource(R.string.feedback_submit), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text,
        color         = CyberTextSecondary,
        fontWeight    = FontWeight.Medium,
        fontSize      = 10.sp,
        letterSpacing = 1.sp,
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ChipRow(values: List<String>, accent: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        values.forEach { NeonChip(it, accent = accent) }
    }
}

@Composable
private fun shimmerBrush(): Brush {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue  = -300f,
        targetValue   = 900f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label         = "sx",
    )
    return Brush.linearGradient(
        colors = listOf(CyberBgCard, CyberBgSurface, CyberBgCard),
        start  = Offset(x, 0f),
        end    = Offset(x + 300f, 0f),
    )
}

@Composable
private fun ShimmerThreatCard() {
    val brush = shimmerBrush()
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp, 18.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth(0.3f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            }
            Box(Modifier.size(48.dp, 14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth(0.75f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .background(
                    Brush.radialGradient(
                        listOf(CyberGreen.copy(alpha = 0.18f), CyberGreen.copy(alpha = 0f))
                    ),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Shield, null, tint = CyberGreen, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.history_empty_title),
            color      = CyberTextPrimary,
            fontSize   = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.history_empty_body),
            color      = CyberTextSecondary,
            fontSize   = 13.sp,
            lineHeight = 20.sp,
            textAlign  = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportBottomSheet(
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onFormat: (ExportFormat) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = CyberBgCard,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CyberTextMuted.copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                stringResource(R.string.export_sheet_title),
                color      = CyberTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
            )
            Spacer(Modifier.height(16.dp))
            if (isExporting) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.export_generating), color = CyberTextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                ExportFormatRow(
                    icon        = Icons.Rounded.PictureAsPdf,
                    title       = stringResource(R.string.export_format_pdf),
                    description = stringResource(R.string.export_format_pdf_desc),
                    accent      = CyberCyan,
                    onClick     = { onFormat(ExportFormat.PDF) },
                )
                Spacer(Modifier.height(8.dp))
                ExportFormatRow(
                    icon        = Icons.Rounded.GridOn,
                    title       = stringResource(R.string.export_format_csv),
                    description = stringResource(R.string.export_format_csv_desc),
                    accent      = CyberGreen,
                    onClick     = { onFormat(ExportFormat.CSV) },
                )
                Spacer(Modifier.height(8.dp))
                ExportFormatRow(
                    icon        = Icons.Rounded.Code,
                    title       = stringResource(R.string.export_format_json),
                    description = stringResource(R.string.export_format_json_desc),
                    accent      = CyberAmber,
                    onClick     = { onFormat(ExportFormat.JSON) },
                )
            }
        }
    }
}

@Composable
private fun ExportFormatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberBgSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title,       color = CyberTextPrimary,   fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(description, color = CyberTextSecondary, fontSize = 11.sp)
        }
    }
}

private fun buildShareIntent(mimeType: String, uri: Uri): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

@Composable
private fun searchFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor   = CyberBgCard,
    unfocusedContainerColor = CyberBgCard,
    focusedTextColor        = CyberTextPrimary,
    unfocusedTextColor      = CyberTextPrimary,
    cursorColor             = CyberCyan,
    focusedIndicatorColor   = CyberCyan,
    unfocusedIndicatorColor = CyberBgSurface,
    focusedLeadingIconColor  = CyberCyan,
    unfocusedLeadingIconColor = CyberTextSecondary,
)
