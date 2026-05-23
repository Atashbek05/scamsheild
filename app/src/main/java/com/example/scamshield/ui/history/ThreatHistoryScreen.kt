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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
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
import com.example.scamshield.R
import com.example.scamshield.data.RiskLevel
import com.example.scamshield.data.db.ThreatEntity
import com.example.scamshield.data.riskLevelEnum
import com.example.scamshield.data.splitFromDb
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.components.NeonChip
import com.example.scamshield.ui.components.RiskBar
import com.example.scamshield.ui.components.riskColor
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Fmt = SimpleDateFormat("MMM dd · HH:mm", Locale.US)

@Composable
fun ThreatHistoryScreen() {
    val vm: ThreatHistoryViewModel = viewModel(factory = ThreatHistoryViewModel.Factory)
    val filters by vm.filters.collectAsStateWithLifecycle()
    val threats by vm.threats.collectAsStateWithLifecycle()
    val packages by vm.packages.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize().background(CyberBgDeep),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { HeaderRow(threats.size) { vm.clearAll() } }
        item {
            OutlinedTextField(
                value = filters.query,
                onValueChange = { vm.setQuery(it) },
                placeholder = { Text(stringResource(R.string.history_search_placeholder), color = CyberTextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = CyberCyan) },
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        Icon(Icons.Filled.Close, null, tint = CyberTextSecondary,
                            modifier = Modifier.clickable { vm.setQuery("") })
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = textFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { FilterRow(filters, packages, vm) }

        if (threats.isEmpty()) {
            item { EmptyState() }
        } else {
            items(threats, key = { it.id }) { ThreatItem(it, onDelete = { vm.deleteThreat(it.id) }) }
        }
    }
}

@Composable
private fun HeaderRow(count: Int, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.history_title), color = CyberCyan, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 3.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onClear) {
            Icon(Icons.Filled.DeleteForever, null, tint = CyberRed)
            Spacer(Modifier.padding(2.dp))
            Text(stringResource(R.string.history_clear), color = CyberRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
    Text(stringResource(R.string.history_record_count, count), color = CyberTextSecondary, fontSize = 11.sp)
}

@Composable
private fun FilterRow(
    filters: ThreatHistoryViewModel.Filters,
    packages: List<String>,
    vm: ThreatHistoryViewModel,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThreatHistoryViewModel.DateRange.entries.forEach { range ->
                FilterChip(label = stringResource(range.labelRes), selected = filters.range == range) {
                    vm.setRange(range)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(stringResource(R.string.history_filter_all_apps), filters.app == null) { vm.setApp(null) }
                packages.forEach { pkg ->
                    val label = pkg.substringAfterLast('.')
                    FilterChip(label, filters.app == pkg) { vm.setApp(pkg) }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) CyberCyan.copy(alpha = 0.18f) else CyberBgCard
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, color = if (selected) CyberCyan else CyberTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ThreatItem(threat: ThreatEntity, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val risk = threat.riskLevelEnum()
    CyberCard(accent = riskColor(risk)) {
        Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        threat.appLabel.ifBlank { threat.sourcePackage.substringAfterLast('.') }.uppercase(),
                        color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(Fmt.format(Date(threat.timestamp)), color = CyberTextSecondary, fontSize = 10.sp)
                }
                Text(
                    "${(threat.probability * 100).toInt()}% ${stringResource(risk.labelRes).uppercase()}",
                    color = riskColor(risk), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, tint = CyberTextSecondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(threat.messagePreview, color = CyberTextPrimary, fontSize = 12.sp,
                maxLines = if (expanded) 8 else 2, overflow = TextOverflow.Ellipsis)
            AnimatedVisibility(expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    RiskBar(probability = threat.probability, riskLevel = risk, showLabel = true)
                    Spacer(Modifier.height(10.dp))

                    if (threat.keywords.isNotBlank()) {
                        SectionLabel(stringResource(R.string.history_section_keywords))
                        ChipFlow(threat.keywords.splitFromDb(), riskColor(risk))
                    }
                    if (threat.phishingLinks.isNotBlank()) {
                        SectionLabel(stringResource(R.string.history_section_links))
                        ChipFlow(threat.phishingLinks.splitFromDb(), CyberRed)
                    }
                    if (threat.urgencyPatterns.isNotBlank()) {
                        SectionLabel(stringResource(R.string.history_section_urgency))
                        ChipFlow(threat.urgencyPatterns.splitFromDb(), com.example.scamshield.ui.theme.CyberAmber)
                    }
                    if (threat.socialIndicators.isNotBlank()) {
                        SectionLabel(stringResource(R.string.history_section_social))
                        ChipFlow(threat.socialIndicators.splitFromDb(), com.example.scamshield.ui.theme.CyberAmber)
                    }
                    if (threat.explanation.isNotBlank()) {
                        SectionLabel(stringResource(R.string.history_section_ai))
                        Text(threat.explanation, color = CyberTextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Filled.DeleteForever, null, tint = CyberRed, modifier = Modifier.padding(end = 4.dp))
                            Text(stringResource(R.string.history_delete), color = CyberRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
    Text(text, color = CyberTextSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.5.sp)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ChipFlow(values: List<String>, accent: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { NeonChip(it, accent = accent) }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.history_empty), color = CyberTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor   = CyberBgCard,
    unfocusedContainerColor = CyberBgCard,
    focusedTextColor        = CyberTextPrimary,
    unfocusedTextColor      = CyberTextPrimary,
    cursorColor             = CyberCyan,
    focusedIndicatorColor   = CyberCyan,
    unfocusedIndicatorColor = CyberBgSurface,
)
