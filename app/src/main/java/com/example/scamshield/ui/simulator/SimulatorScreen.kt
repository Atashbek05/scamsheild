package com.example.scamshield.ui.simulator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamshield.R
import com.example.scamshield.simulator.ScamSamples
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.components.NeonChip
import com.example.scamshield.ui.components.NeonProgressBar
import com.example.scamshield.ui.components.RiskBar
import com.example.scamshield.ui.components.riskColor
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary

@Composable
fun SimulatorScreen() {
    val vm: SimulatorViewModel = viewModel(factory = SimulatorViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val showOverlay by vm.showOverlay.collectAsStateWithLifecycle()
    var selectedType by remember { mutableStateOf<ScamSamples.Type?>(null) }
    var customMessage by remember { mutableStateOf("") }
    var customSender by remember { mutableStateOf("") }

    val visibleSamples = remember(selectedType) {
        if (selectedType == null) vm.samples else vm.samples.filter { it.type == selectedType }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BugReport, null, tint = CyberRed, modifier = Modifier.size(26.dp))
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.simulator_title), color = CyberTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Text(stringResource(R.string.simulator_subtitle), color = CyberTextSecondary, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(14.dp))

        CyberCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.simulator_show_overlay), color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.simulator_overlay_requires), color = CyberTextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = showOverlay,
                    onCheckedChange = vm::setShowOverlay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberGreen,
                        checkedTrackColor = CyberGreen.copy(alpha = 0.35f),
                    ),
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryFilter(stringResource(R.string.simulator_filter_all), selectedType == null) { selectedType = null }
            ScamSamples.Type.entries.forEach { type ->
                CategoryFilter(stringResource(type.labelRes), selectedType == type) { selectedType = type }
            }
        }
        Spacer(Modifier.height(12.dp))

        visibleSamples.forEach { sample ->
            SampleCard(sample) { vm.runSample(sample) }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))

        CyberCard(accent = CyberGreen) {
            Text(stringResource(R.string.simulator_custom_attack), color = CyberGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customSender,
                onValueChange = { customSender = it },
                label = { Text(stringResource(R.string.simulator_sender_label), color = CyberTextSecondary, fontSize = 11.sp) },
                singleLine = true,
                colors = textFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customMessage,
                onValueChange = { customMessage = it },
                label = { Text(stringResource(R.string.simulator_message_label), color = CyberTextSecondary, fontSize = 11.sp) },
                minLines = 3,
                colors = textFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { vm.runCustom(customMessage, customSender) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = CyberBgDeep),
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.simulator_launch), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))

        ResultPanel(state, onReset = vm::reset)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun CategoryFilter(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun SampleCard(sample: ScamSamples.Sample, onLaunch: () -> Unit) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    sample.title,
                    color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(sample.type.labelRes).uppercase(),
                    color = CyberCyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBgDeep),
            ) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.simulator_run), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.simulator_from_sender, sample.sender), color = CyberTextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Text(sample.message, color = CyberTextPrimary, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 4)
    }
}

@Composable
private fun ResultPanel(state: SimulatorViewModel.State, onReset: () -> Unit) {
    when (state) {
        is SimulatorViewModel.State.Idle -> Unit
        is SimulatorViewModel.State.Loading -> CyberCard {
            Text(stringResource(R.string.simulator_analysing), color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            NeonProgressBar()
        }
        is SimulatorViewModel.State.Error -> CyberCard(accent = CyberRed) {
            Text(state.message, color = CyberRed, fontSize = 13.sp)
        }
        is SimulatorViewModel.State.Success -> CyberCard(accent = riskColor(state.result.riskLevel)) {
            val res = state.result
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.simulator_result), color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberBgCard)
                        .clickable(onClick = onReset)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Refresh, null, tint = CyberTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.simulator_reset), color = CyberTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            RiskBar(probability = res.finalProbability, riskLevel = res.riskLevel)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.simulator_category, stringResource(res.category.labelRes)),
                color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(res.explanation.overallReason, color = CyberTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)

            ExplainSection(stringResource(R.string.simulator_section_keywords), res.explanation.suspiciousKeywords, riskColor(res.riskLevel))
            ExplainSection(stringResource(R.string.simulator_section_links), res.explanation.phishingLinks, CyberRed)
            ExplainSection(stringResource(R.string.simulator_section_urgency), res.explanation.urgencyPatterns, com.example.scamshield.ui.theme.CyberAmber)
            ExplainSection(stringResource(R.string.simulator_section_social), res.explanation.socialEngineeringIndicators, com.example.scamshield.ui.theme.CyberAmber)
        }
    }
}

@Composable
private fun ExplainSection(title: String, values: List<String>, accent: androidx.compose.ui.graphics.Color) {
    if (values.isEmpty()) return
    Spacer(Modifier.height(10.dp))
    Text(title.uppercase(), color = CyberTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.take(10).forEach { NeonChip(it, accent = accent) }
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
