package com.example.scamshield.ui.call

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.ReportGmailerrorred
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamshield.R
import com.example.scamshield.data.call.CallAction
import com.example.scamshield.data.call.CallRisk
import com.example.scamshield.data.db.CallEntity
import com.example.scamshield.data.db.TopScamCallerRow
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.components.NeonChip
import com.example.scamshield.ui.components.NeonPulse
import com.example.scamshield.ui.components.StatTile
import com.example.scamshield.ui.theme.CyberAmber
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberBorder
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TimeFmt = SimpleDateFormat("HH:mm", Locale.US)

@Composable
fun CallProtectionScreen() {
    val vm: CallProtectionViewModel = viewModel(factory = CallProtectionViewModel.Factory)

    val recent by vm.recentCalls.collectAsStateWithLifecycle()
    val totalScreened by vm.totalScreened.collectAsStateWithLifecycle()
    val riskyCount by vm.riskyCount.collectAsStateWithLifecycle()
    val blockedCount by vm.blockedCount.collectAsStateWithLifecycle()
    val todayCount by vm.today.collectAsStateWithLifecycle()
    val todayRisky by vm.todayRisky.collectAsStateWithLifecycle()
    val topCallers by vm.topCallers.collectAsStateWithLifecycle()
    val blocked by vm.blocked.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        ProtectionHeader(activeBlocks = blockedCount, todayRisky = todayRisky)
        Spacer(Modifier.height(12.dp))

        // ── Top stats grid ────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = stringResource(R.string.call_stat_today),
                value = todayCount.toString(),
                accent = CyberCyan,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.call_stat_today_threats),
                value = todayRisky.toString(),
                accent = CyberRed,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = stringResource(R.string.call_stat_total),
                value = totalScreened.toString(),
                accent = CyberGreen,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.call_stat_blocked),
                value = blockedCount.toString(),
                accent = CyberAmber,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(14.dp))
        TopScamCallersCard(topCallers, onBlock = { vm.block(it) })

        Spacer(Modifier.height(14.dp))
        CallHistoryCard(recent, parseFlags = vm::parseFlags, onBlock = { vm.block(it) })

        Spacer(Modifier.height(14.dp))
        BlockedListCard(blocked, onUnblock = { vm.unblock(it) })

        Spacer(Modifier.height(20.dp))
    }
}

// ── Header strip with pulsing shield ─────────────────────────────────────────

@Composable
private fun ProtectionHeader(activeBlocks: Int, todayRisky: Int) {
    val danger = todayRisky > 0
    val accent = if (danger) CyberRed else CyberGreen
    CyberCard(accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.call_header_title),
                    color = CyberTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(
                        if (danger) R.string.call_header_active_dangerous
                        else R.string.call_header_active_safe
                    ),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            NeonPulse(color = accent)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DangerPulse(accent = accent, modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.call_header_subline, activeBlocks),
                color = CyberTextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun DangerPulse(accent: Color, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "danger_pulse")
    val alpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = alpha)),
    )
}

// ── Top scam callers ──────────────────────────────────────────────────────────

@Composable
private fun TopScamCallersCard(
    rows: List<TopScamCallerRow>,
    onBlock: (String) -> Unit,
) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = CyberRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.call_top_scammers_title),
                color = CyberTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.call_top_scammers_subtitle),
            color = CyberTextSecondary,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(10.dp))

        if (rows.isEmpty()) {
            EmptyState(stringResource(R.string.call_top_scammers_empty))
        } else {
            rows.forEach { row ->
                ScamCallerRow(row, onBlock = onBlock)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ScamCallerRow(row: TopScamCallerRow, onBlock: (String) -> Unit) {
    val pct = (row.maxProbability * 100).toInt()
    val accent = colorFor(row.maxProbability)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBgSurface)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                row.displayName.ifBlank { row.phoneNumber.ifBlank { "—" } },
                color = CyberTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                row.phoneNumber,
                color = CyberTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$pct%", color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.call_top_scammers_count, row.callCount),
                color = CyberTextMuted,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        SmallActionPill(
            label = stringResource(R.string.call_action_block),
            accent = CyberRed,
            onClick = { onBlock(row.phoneNumber) },
        )
    }
}

// ── Call history list ────────────────────────────────────────────────────────

@Composable
private fun CallHistoryCard(
    rows: List<CallEntity>,
    parseFlags: (String) -> List<com.example.scamshield.data.call.CallFlag>,
    onBlock: (String) -> Unit,
) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Call, null, tint = CyberCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.call_history_title),
                color = CyberTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.call_history_count, rows.size),
                color = CyberTextMuted,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.height(10.dp))

        if (rows.isEmpty()) {
            EmptyState(stringResource(R.string.call_history_empty))
        } else {
            rows.forEach { call ->
                CallHistoryRow(call, parseFlags = parseFlags, onBlock = onBlock)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CallHistoryRow(
    call: CallEntity,
    parseFlags: (String) -> List<com.example.scamshield.data.call.CallFlag>,
    onBlock: (String) -> Unit,
) {
    val accent = colorFor(call.probability)
    val risk = runCatching { CallRisk.valueOf(call.risk) }.getOrDefault(CallRisk.SAFE)
    val flags = parseFlags(call.flags)
    val action = runCatching { CallAction.valueOf(call.action) }.getOrDefault(CallAction.ALLOWED)
    val pct = (call.probability * 100).toInt()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBgSurface)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (action) {
                    CallAction.BLOCKED  -> Icons.Filled.Block
                    CallAction.REPORTED -> Icons.Filled.ReportGmailerrorred
                    CallAction.SCREENED -> Icons.Filled.Warning
                    CallAction.ALLOWED  -> Icons.Filled.Call
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    call.displayName.ifBlank { call.phoneNumber.ifBlank { "—" } },
                    color = CyberTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${call.phoneNumber}  ·  ${TimeFmt.format(Date(call.timestamp))}",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("$pct%", color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        if (flags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            // Compact wrap-row of flag chips (cap at 3 — overflow chip shows the rest)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                flags.take(3).forEach { flag ->
                    NeonChip(text = flagShortLabel(flag), accent = accent)
                }
                if (flags.size > 3) {
                    NeonChip(text = "+${flags.size - 3}", accent = CyberTextSecondary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(risk.labelRes).uppercase(),
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            if (action != CallAction.BLOCKED && call.phoneNumber.isNotBlank()) {
                SmallActionPill(
                    label = stringResource(R.string.call_action_block),
                    accent = CyberRed,
                    onClick = { onBlock(call.phoneNumber) },
                )
            }
        }
    }
}

// ── Blocked list ─────────────────────────────────────────────────────────────

@Composable
private fun BlockedListCard(
    blocked: List<com.example.scamshield.data.db.BlockedNumberEntity>,
    onUnblock: (String) -> Unit,
) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.PhoneDisabled, null, tint = CyberRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.call_blocked_title),
                color = CyberTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.call_blocked_count, blocked.size),
                color = CyberTextMuted,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        if (blocked.isEmpty()) {
            EmptyState(stringResource(R.string.call_blocked_empty))
        } else {
            blocked.forEach { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberBgSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.phoneNumber,
                            color = CyberTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            entry.reason,
                            color = CyberTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                    SmallActionPill(
                        label = stringResource(R.string.call_action_unblock),
                        accent = CyberCyan,
                        onClick = { onUnblock(entry.phoneNumber) },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Shared bits ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBgSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = CyberTextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun SmallActionPill(label: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.15f))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label.uppercase(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun colorFor(probability: Float): Color = when (CallRisk.fromProbability(probability)) {
    CallRisk.CRITICAL, CallRisk.HIGH -> CyberRed
    CallRisk.MEDIUM                  -> CyberAmber
    CallRisk.LOW                     -> CyberGreen
    CallRisk.SAFE, CallRisk.TRUSTED  -> CyberGreen
}

/** Short label for a CallFlag — picks a concise word for chip display. */
@Composable
private fun flagShortLabel(flag: com.example.scamshield.data.call.CallFlag): String = stringResource(
    when (flag) {
        com.example.scamshield.data.call.CallFlag.UNKNOWN_NUMBER       -> R.string.call_flag_chip_unknown
        com.example.scamshield.data.call.CallFlag.REPEATED_UNKNOWN     -> R.string.call_flag_chip_repeat
        com.example.scamshield.data.call.CallFlag.INTERNATIONAL_PREFIX -> R.string.call_flag_chip_intl
        com.example.scamshield.data.call.CallFlag.HIGH_RISK_PREFIX     -> R.string.call_flag_chip_risk_prefix
        com.example.scamshield.data.call.CallFlag.SHORT_NUMBER         -> R.string.call_flag_chip_short
        com.example.scamshield.data.call.CallFlag.FAKE_BANK_PATTERN    -> R.string.call_flag_chip_bank
        com.example.scamshield.data.call.CallFlag.SCAM_KEYWORD         -> R.string.call_flag_chip_scam
        com.example.scamshield.data.call.CallFlag.ROBOCALL_BEHAVIOR    -> R.string.call_flag_chip_robocall
        com.example.scamshield.data.call.CallFlag.SHORT_DURATION_SPAM  -> R.string.call_flag_chip_duration
        com.example.scamshield.data.call.CallFlag.REPORTED_NUMBER      -> R.string.call_flag_chip_reported
        com.example.scamshield.data.call.CallFlag.BLOCKED_DB_MATCH     -> R.string.call_flag_chip_blocked
        com.example.scamshield.data.call.CallFlag.NO_CALLER_ID         -> R.string.call_flag_chip_hidden
    }
)
