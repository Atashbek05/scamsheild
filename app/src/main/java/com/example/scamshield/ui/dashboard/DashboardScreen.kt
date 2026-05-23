package com.example.scamshield.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamshield.R
import com.example.scamshield.data.AiConnectionState
import com.example.scamshield.data.MonitoringEvent
import com.example.scamshield.data.RiskLevel
import com.example.scamshield.data.ScanStatus
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.components.InlineMetrics
import com.example.scamshield.ui.components.NeonChip
import com.example.scamshield.ui.components.NeonProgressBar
import com.example.scamshield.ui.components.NeonPulse
import com.example.scamshield.ui.components.StatTile
import com.example.scamshield.ui.components.riskColor
import com.example.scamshield.ui.theme.CyberAmber
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DateFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

@Composable
fun DashboardScreen(
    isNotificationGranted: Boolean,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    onGrantNotificationClick: () -> Unit,
    onGrantAccessibilityClick: () -> Unit,
    onGrantOverlayClick: () -> Unit,
    onRefreshPermissions: () -> Unit,
) {
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val liveEvent by vm.liveEvent.collectAsStateWithLifecycle()
    val recentThreats by vm.recentThreatsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentScans by vm.recentScans.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        HeaderBlock(onRefresh = onRefreshPermissions)
        Spacer(Modifier.height(14.dp))

        ProtectionRing(state)
        Spacer(Modifier.height(16.dp))

        StatsGrid(state)
        Spacer(Modifier.height(16.dp))

        LiveMonitorPanel(liveEvent, recentScans, state.aiState)
        Spacer(Modifier.height(16.dp))

        if (!(isNotificationGranted && isAccessibilityGranted && isOverlayGranted)) {
            PermissionsBanner(
                isNotificationGranted = isNotificationGranted,
                isAccessibilityGranted = isAccessibilityGranted,
                isOverlayGranted = isOverlayGranted,
                onGrantNotificationClick = onGrantNotificationClick,
                onGrantAccessibilityClick = onGrantAccessibilityClick,
                onGrantOverlayClick = onGrantOverlayClick,
            )
            Spacer(Modifier.height(16.dp))
        }

        AiEngineCard(state, onRunTest = vm::runAiTest)
        Spacer(Modifier.height(16.dp))

        RecentThreatsCard(recentThreats)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun HeaderBlock(onRefresh: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .background(CyberCyan.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Shield, null, tint = CyberCyan)
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.dashboard_title), color = CyberTextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 3.sp)
            Text(stringResource(R.string.dashboard_subtitle), color = CyberTextSecondary, fontSize = 11.sp)
        }
        Box(
            Modifier
                .clip(CircleShape)
                .background(CyberBgCard)
                .clickable(onClick = onRefresh)
                .padding(8.dp),
        ) {
            Icon(Icons.Filled.Refresh, null, tint = CyberTextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProtectionRing(state: DashboardViewModel.State) {
    val online = state.aiState is AiConnectionState.Online
    val accent = if (online && state.activeServiceCount > 0) CyberGreen else CyberAmber
    val infinite = rememberInfiniteTransition(label = "ring")
    val sweep by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "sweep",
    )
    CyberCard(accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(86.dp)) {
                    val s = size.minDimension
                    drawCircle(accent.copy(alpha = 0.12f), s / 2f - 4f)
                    drawArc(
                        color = accent,
                        startAngle = sweep,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
                        topLeft = androidx.compose.ui.geometry.Offset(4f, 4f),
                        size = androidx.compose.ui.geometry.Size(s - 8f, s - 8f),
                    )
                }
                Icon(Icons.Filled.Shield, null, tint = accent, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (online) R.string.dashboard_active_protection else R.string.dashboard_standby),
                    color = accent, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        online && state.activeServiceCount >= 2 ->
                            stringResource(R.string.dashboard_ai_online_engines, state.activeServiceCount)
                        online ->
                            stringResource(R.string.dashboard_ai_online_waiting)
                        state.aiState is AiConnectionState.Connecting ->
                            stringResource(R.string.dashboard_connecting)
                        state.aiState is AiConnectionState.Offline ->
                            stringResource(R.string.dashboard_backend_offline)
                        else ->
                            stringResource(R.string.dashboard_initialising)
                    },
                    color = CyberTextSecondary, fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                InlineMetrics(
                    items = listOf(
                        stringResource(R.string.dashboard_metric_scanned) to state.totalScanned.toString(),
                        stringResource(R.string.dashboard_metric_threats) to state.totalThreats.toString(),
                        stringResource(R.string.dashboard_metric_high_risk) to state.highRiskCount.toString(),
                    ),
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(state: DashboardViewModel.State) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(stringResource(R.string.stat_scanned), state.totalScanned.toString(), CyberCyan, Modifier.weight(1f))
        StatTile(stringResource(R.string.stat_threats), state.totalThreats.toString(), CyberRed, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(stringResource(R.string.stat_high_risk), state.highRiskCount.toString(), CyberRed, Modifier.weight(1f))
        StatTile(stringResource(R.string.stat_today), state.threatsToday.toString(), CyberAmber, Modifier.weight(1f))
    }
}

@Composable
private fun LiveMonitorPanel(
    live: MonitoringEvent?,
    recentScans: List<MonitoringEvent>,
    ai: AiConnectionState,
) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Sensors, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.dashboard_live_monitor), color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            NeonPulse(color = if (live != null) CyberGreen else CyberTextSecondary, diameter = 6.dp)
        }
        Spacer(Modifier.height(10.dp))

        if (live != null) {
            ScanRow(live)
            Spacer(Modifier.height(8.dp))
            NeonProgressBar()
        } else {
            Text(
                stringResource(if (ai is AiConnectionState.Online) R.string.dashboard_no_scan else R.string.dashboard_engine_idle),
                color = CyberTextSecondary, fontSize = 12.sp,
            )
        }

        if (recentScans.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.dashboard_recent_activity), color = CyberTextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(6.dp))
            recentScans.take(5).forEach { ev ->
                ScanRow(ev)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ScanRow(ev: MonitoringEvent) {
    val color = when (ev.status) {
        ScanStatus.SCANNING        -> CyberCyan
        ScanStatus.ANALYZING       -> CyberCyan
        ScanStatus.SAFE_MESSAGE    -> CyberGreen
        ScanStatus.THREAT_DETECTED -> CyberRed
        ScanStatus.ERROR           -> CyberAmber
    }
    val tag = when (ev.status) {
        ScanStatus.SCANNING        -> stringResource(R.string.scan_status_scanning)
        ScanStatus.ANALYZING       -> stringResource(R.string.scan_status_analyzing)
        ScanStatus.SAFE_MESSAGE    -> stringResource(R.string.scan_status_safe)
        ScanStatus.THREAT_DETECTED -> stringResource(R.string.scan_status_threat)
        ScanStatus.ERROR           -> stringResource(R.string.scan_status_error)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.size(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ev.appLabel.uppercase(),
                    color = CyberTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(4.dp))
                Text(tag, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, maxLines = 1)
                Spacer(Modifier.size(6.dp))
                Text(DateFmt.format(Date(ev.timestamp)), color = CyberTextSecondary, fontSize = 9.sp)
            }
            Text(ev.textPreview.take(80), color = CyberTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        ev.probability?.let {
            Spacer(Modifier.size(6.dp))
            Text("${(it * 100).toInt()}%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PermissionsBanner(
    isNotificationGranted: Boolean,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    onGrantNotificationClick: () -> Unit,
    onGrantAccessibilityClick: () -> Unit,
    onGrantOverlayClick: () -> Unit,
) {
    CyberCard(accent = CyberAmber) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WarningAmber, null, tint = CyberAmber)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.dashboard_permissions_needed), color = CyberAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.5.sp)
        }
        Spacer(Modifier.height(8.dp))
        PermissionRow(stringResource(R.string.dashboard_perm_notification_access), isNotificationGranted, onGrantNotificationClick)
        PermissionRow(stringResource(R.string.dashboard_perm_accessibility_service), isAccessibilityGranted, onGrantAccessibilityClick)
        PermissionRow(stringResource(R.string.dashboard_perm_overlay_apps), isOverlayGranted, onGrantOverlayClick)
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
            null,
            tint = if (granted) CyberGreen else CyberAmber,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(label, color = CyberTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (!granted) {
            OutlinedButton(onClick = onClick) {
                Text(stringResource(R.string.perm_grant), color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AiEngineCard(state: DashboardViewModel.State, onRunTest: () -> Unit) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Psychology, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.dashboard_ai_engine), color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            val (label, color) = when (state.aiState) {
                is AiConnectionState.Online     -> stringResource(R.string.ai_state_online)     to CyberGreen
                is AiConnectionState.Connecting -> stringResource(R.string.ai_state_connecting) to CyberCyan
                is AiConnectionState.Offline    -> stringResource(R.string.ai_state_offline)    to CyberAmber
                is AiConnectionState.Error      -> stringResource(R.string.ai_state_error)      to CyberRed
                is AiConnectionState.Idle       -> stringResource(R.string.ai_state_idle)       to CyberTextSecondary
            }
            NeonChip(label, accent = color)
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.dashboard_ai_desc), color = CyberTextSecondary, fontSize = 12.sp)
        if (state.aiState is AiConnectionState.Connecting) {
            Spacer(Modifier.height(8.dp))
            NeonProgressBar()
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRunTest,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBgDeep),
        ) {
            Icon(Icons.Filled.PlayArrow, null)
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.dashboard_run_test), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentThreatsCard(recent: List<com.example.scamshield.data.DetectedThreat>) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Bolt, null, tint = CyberRed, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.dashboard_recent_threats), color = CyberRed, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
            Text("${recent.size}", color = CyberTextSecondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        if (recent.isEmpty()) {
            Text(stringResource(R.string.dashboard_no_threats), color = CyberTextSecondary, fontSize = 12.sp)
        } else {
            recent.take(5).forEach { threat ->
                ThreatRow(threat)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ThreatRow(threat: com.example.scamshield.data.DetectedThreat) {
    val risk = RiskLevel.fromProbability(threat.probability)
    val color = riskColor(risk)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberBgCard)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                threat.appLabel ?: threat.sourcePackage.substringAfterLast('.').uppercase(),
                color = CyberTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(4.dp))
            Text(
                "${(threat.probability * 100).toInt()}% ${stringResource(risk.labelRes)}",
                color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(threat.messagePreview, color = CyberTextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (threat.keywords.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                threat.keywords.take(3).forEach { NeonChip(it, accent = color) }
            }
        }
    }
}
