package com.example.scamshield.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.components.NeonChip
import com.example.scamshield.ui.components.RiskBar
import com.example.scamshield.ui.components.riskColor
import com.example.scamshield.ui.theme.CyberAmber
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary
import com.example.scamshield.ui.theme.CyberTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Fmt = SimpleDateFormat("MMM dd · HH:mm", Locale.US)

@Composable
fun ThreatHistoryScreen() {
    val vm       = viewModel<ThreatHistoryViewModel>(factory = ThreatHistoryViewModel.Factory)
    val filters  by vm.filters.collectAsStateWithLifecycle()
    val threats  = vm.threats.collectAsLazyPagingItems()
    val packages by vm.packages.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep),
        contentPadding    = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenHeader(threats.itemCount) { vm.clearAll() } }

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

        item {
            when (threats.loadState.refresh) {
                is LoadState.Loading -> CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(24.dp))
                is LoadState.Error   -> Text(stringResource(R.string.history_empty), color = CyberRed, fontSize = 12.sp)
                else                 -> {}
            }
        }

        if (threats.itemCount == 0 && threats.loadState.refresh !is LoadState.Loading) {
            item { EmptyState() }
        } else {
            items(count = threats.itemCount) { index ->
                val threat = threats[index] ?: return@items
                ThreatItem(threat = threat, onDelete = { vm.deleteThreat(threat.id) })
            }
        }
    }
}

@Composable
private fun ScreenHeader(count: Int, onClear: () -> Unit) {
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

@Composable
private fun ThreatItem(threat: ThreatEntity, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val risk  = threat.riskLevelEnum()
    val color = riskColor(risk)

    CyberCard(accent = color) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Risk badge
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
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
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
private fun EmptyState() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Search, null, tint = CyberTextMuted, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.history_empty), color = CyberTextSecondary, fontSize = 13.sp)
        }
    }
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
