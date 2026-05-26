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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SignalWifiOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TimeFmt = SimpleDateFormat("HH:mm", Locale.US)

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
    val state        by vm.state.collectAsStateWithLifecycle()
    val liveEvent    by vm.liveEvent.collectAsStateWithLifecycle()
    val recentThreats by vm.recentThreatsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val recentScans  by vm.recentScans.collectAsStateWithLifecycle()

    var isInitialLoad by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(600); isInitialLoad = false }

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        TopBar(onRefresh = onRefreshPermissions)
        Spacer(Modifier.height(16.dp))

        if (isInitialLoad) SkeletonHeroCard() else ProtectionHero(state)
        Spacer(Modifier.height(14.dp))

        if (isInitialLoad) SkeletonStatsGrid() else StatsGrid(state)
        Spacer(Modifier.height(14.dp))

        LiveMonitorCard(liveEvent, recentScans, state.aiState)
        Spacer(Modifier.height(14.dp))

        if (!(isNotificationGranted && isAccessibilityGranted && isOverlayGranted)) {
            PermissionsBanner(
                isNotificationGranted  = isNotificationGranted,
                isAccessibilityGranted = isAccessibilityGranted,
                isOverlayGranted       = isOverlayGranted,
                onGrantNotificationClick  = onGrantNotificationClick,
                onGrantAccessibilityClick = onGrantAccessibilityClick,
                onGrantOverlayClick       = onGrantOverlayClick,
            )
            Spacer(Modifier.height(14.dp))
        }

        AiEngineCard(state, onRunTest = vm::runAiTest)
        Spacer(Modifier.height(14.dp))

        RecentThreatsCard(recentThreats)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TopBar(onRefresh: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(38.dp)
                .background(CyberCyan.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Shield, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.dashboard_title),
                color         = CyberTextPrimary,
                fontWeight    = FontWeight.Black,
                fontSize      = 18.sp,
                letterSpacing = 2.sp,
            )
            Text(
                stringResource(R.string.dashboard_subtitle),
                color    = CyberTextSecondary,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
            )
        }
        Box(
            Modifier
                .clip(CircleShape)
                .background(CyberBgSurface)
                .clickable(onClick = onRefresh)
                .padding(8.dp),
        ) {
            Icon(Icons.Rounded.Refresh, null, tint = CyberTextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ProtectionHero(state: DashboardViewModel.State) {
    val isConnected = state.aiState is AiConnectionState.Online || state.aiState is AiConnectionState.Slow
    val accent = when {
        state.aiState is AiConnectionState.Online && state.activeServiceCount > 0 -> CyberGreen
        state.aiState is AiConnectionState.Slow        -> CyberCyan
        state.aiState is AiConnectionState.RateLimited -> CyberAmber
        state.aiState is AiConnectionState.Error       -> CyberRed
        else                                           -> CyberAmber
    }
    val infinite = rememberInfiniteTransition(label = "ring")
    val sweep by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(4500, easing = LinearEasing)),
        label = "sweep",
    )

    CyberCard(accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Animated shield ring
            Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(92.dp)) {
                    val s = size.minDimension
                    val r = s / 2f - 4f
                    drawCircle(accent.copy(alpha = 0.08f), r)
                    drawArc(
                        color      = accent,
                        startAngle = sweep,
                        sweepAngle = 110f,
                        useCenter  = false,
                        style      = Stroke(width = 3f, cap = StrokeCap.Round),
                        topLeft    = Offset(4f, 4f),
                        size       = Size(s - 8f, s - 8f),
                    )
                    drawArc(
                        color      = accent.copy(alpha = 0.25f),
                        startAngle = sweep + 180f,
                        sweepAngle = 60f,
                        useCenter  = false,
                        style      = Stroke(width = 2f, cap = StrokeCap.Round),
                        topLeft    = Offset(4f, 4f),
                        size       = Size(s - 8f, s - 8f),
                    )
                    drawCircle(accent.copy(alpha = 0.15f), r * 0.60f, style = Stroke(1.2f))
                }
                Icon(Icons.Rounded.Shield, null, tint = accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (isConnected) R.string.dashboard_active_protection else R.string.dashboard_standby),
                    color         = accent,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 15.sp,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    when {
                        state.aiState is AiConnectionState.Online && state.activeServiceCount >= 2 ->
                            stringResource(R.string.dashboard_ai_online_engines, state.activeServiceCount)
                        state.aiState is AiConnectionState.Online ->
                            stringResource(R.string.dashboard_ai_online_waiting)
                        state.aiState is AiConnectionState.Slow ->
                            stringResource(R.string.dashboard_slow_response)
                        state.aiState is AiConnectionState.RateLimited ->
                            stringResource(R.string.dashboard_server_rate_limited)
                        state.aiState is AiConnectionState.Connecting ->
                            stringResource(R.string.dashboard_connecting)
                        state.aiState is AiConnectionState.Offline ->
                            stringResource(R.string.dashboard_backend_offline)
                        state.aiState is AiConnectionState.Throttled ->
                            stringResource(R.string.dashboard_rate_limited)
                        else ->
                            stringResource(R.string.dashboard_initialising)
                    },
                    color    = CyberTextSecondary,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))
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
        StatTile(stringResource(R.string.stat_scanned),   state.totalScanned.toString(),  CyberCyan,  Modifier.weight(1f))
        StatTile(stringResource(R.string.stat_threats),   state.totalThreats.toString(),  CyberRed,   Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(stringResource(R.string.stat_high_risk), state.highRiskCount.toString(), CyberRed,   Modifier.weight(1f))
        StatTile(stringResource(R.string.stat_today),     state.threatsToday.toString(),  CyberAmber, Modifier.weight(1f))
    }
}

@Composable
private fun LiveMonitorCard(
    live: MonitoringEvent?,
    recentScans: List<MonitoringEvent>,
    ai: AiConnectionState,
) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Sensors, null, tint = CyberCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.dashboard_live_monitor),
                color         = CyberCyan,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 12.sp,
                letterSpacing = 1.sp,
                modifier      = Modifier.weight(1f),
            )
            NeonPulse(color = if (live != null) CyberGreen else CyberTextSecondary, diameter = 5.dp)
        }
        Spacer(Modifier.height(12.dp))

        if (live != null) {
            ScanRow(live)
            Spacer(Modifier.height(8.dp))
            NeonProgressBar()
        } else {
            Text(
                stringResource(
                    when (ai) {
                        is AiConnectionState.Online,
                        is AiConnectionState.Slow,
                        is AiConnectionState.RateLimited -> R.string.dashboard_no_scan
                        else -> R.string.dashboard_engine_idle
                    },
                ),
                color    = CyberTextSecondary,
                fontSize = 12.sp,
            )
        }

        if (recentScans.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.dashboard_recent_activity),
                color         = CyberTextSecondary,
                fontWeight    = FontWeight.Medium,
                fontSize      = 10.sp,
                letterSpacing = 1.sp,
            )
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
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.size(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ev.appLabel.uppercase(),
                    color      = CyberTextPrimary,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f),
                )
                Spacer(Modifier.size(4.dp))
                Text(tag, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(Modifier.size(6.dp))
                Text(TimeFmt.format(Date(ev.timestamp)), color = CyberTextSecondary, fontSize = 9.sp)
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
            Icon(Icons.Rounded.Warning, null, tint = CyberAmber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.dashboard_permissions_needed),
                color         = CyberAmber,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 12.sp,
                letterSpacing = 0.5.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        PermissionRow(stringResource(R.string.dashboard_perm_notification_access),   isNotificationGranted,  onGrantNotificationClick)
        PermissionRow(stringResource(R.string.dashboard_perm_accessibility_service), isAccessibilityGranted, onGrantAccessibilityClick)
        PermissionRow(stringResource(R.string.dashboard_perm_overlay_apps),          isOverlayGranted,       onGrantOverlayClick)
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            null,
            tint     = if (granted) CyberGreen else CyberAmber,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(label, color = CyberTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (!granted) {
            OutlinedButton(
                onClick = onClick,
                shape   = RoundedCornerShape(8.dp),
            ) {
                Text(stringResource(R.string.perm_grant), color = CyberCyan, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AiEngineCard(state: DashboardViewModel.State, onRunTest: () -> Unit) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Psychology, null, tint = CyberCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.dashboard_ai_engine),
                color         = CyberCyan,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 12.sp,
                letterSpacing = 1.sp,
                modifier      = Modifier.weight(1f),
            )
            data class StateInfo(val label: String, val color: Color, val icon: ImageVector)
            val stateInfo = when (state.aiState) {
                is AiConnectionState.Online      -> StateInfo(stringResource(R.string.ai_state_online),        CyberGreen,         Icons.Rounded.CheckCircle)
                is AiConnectionState.Slow        -> StateInfo(stringResource(R.string.ai_state_slow),          CyberCyan,          Icons.Rounded.HourglassEmpty)
                is AiConnectionState.RateLimited -> StateInfo(stringResource(R.string.ai_state_rate_limited),  CyberAmber,         Icons.Rounded.Block)
                is AiConnectionState.Connecting  -> StateInfo(stringResource(R.string.ai_state_connecting),    CyberCyan,          Icons.Rounded.Sync)
                is AiConnectionState.Offline     -> StateInfo(stringResource(R.string.ai_state_offline),       CyberAmber,         Icons.Rounded.SignalWifiOff)
                is AiConnectionState.Throttled   -> StateInfo(stringResource(R.string.ai_state_throttled),     CyberAmber,         Icons.Rounded.Block)
                is AiConnectionState.Error       -> StateInfo(stringResource(R.string.ai_state_error),         CyberRed,           Icons.Rounded.ErrorOutline)
                is AiConnectionState.Idle        -> StateInfo(stringResource(R.string.ai_state_idle),          CyberTextSecondary, Icons.Rounded.Sensors)
            }
            Icon(stateInfo.icon, null, tint = stateInfo.color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            NeonChip(stateInfo.label, accent = stateInfo.color)
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.dashboard_ai_desc), color = CyberTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        if (state.aiState is AiConnectionState.Connecting) {
            Spacer(Modifier.height(8.dp))
            NeonProgressBar()
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick  = onRunTest,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberBgDeep),
        ) {
            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.dashboard_run_test), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun RecentThreatsCard(recent: List<com.example.scamshield.data.DetectedThreat>) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, null, tint = CyberRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.dashboard_recent_threats),
                color         = CyberRed,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 12.sp,
                letterSpacing = 1.sp,
                modifier      = Modifier.weight(1f),
            )
            if (recent.isNotEmpty()) {
                Box(
                    Modifier
                        .background(CyberRed.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("${recent.size}", color = CyberRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (recent.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(CyberGreen.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = CyberGreen, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.dashboard_no_active_threats),
                    color      = CyberGreen,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
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
    val risk  = RiskLevel.fromProbability(threat.probability)
    val color = riskColor(risk)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.06f)),
    ) {
        // Left accent bar fills card height
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(color)
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    threat.appLabel ?: threat.sourcePackage.substringAfterLast('.').uppercase(),
                    color      = CyberTextPrimary,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "${(threat.probability * 100).toInt()}% ${stringResource(risk.labelRes).uppercase()}",
                    color      = color,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(threat.messagePreview, color = CyberTextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (threat.keywords.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    threat.keywords.take(3).forEach { NeonChip(it, accent = color) }
                }
            }
        }
    }
}

// ── Skeleton / shimmer loading state ─────────────────────────────────────────

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
private fun SkeletonHeroCard() {
    val brush = shimmerBrush()
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberBgCard)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp).clip(CircleShape).background(brush))
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth(0.55f).height(14.dp).clip(RoundedCornerShape(5.dp)).background(brush))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.8f).height(11.dp).clip(RoundedCornerShape(5.dp)).background(brush))
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(brush))
            }
        }
    }
}

@Composable
private fun SkeletonStatsGrid() {
    val brush = shimmerBrush()
    @Composable fun Tile(mod: Modifier) {
        Box(
            mod
                .clip(RoundedCornerShape(12.dp))
                .background(CyberBgCard)
                .padding(14.dp),
        ) {
            Column {
                Box(Modifier.fillMaxWidth(0.55f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.4f).height(26.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile(Modifier.weight(1f)); Tile(Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile(Modifier.weight(1f)); Tile(Modifier.weight(1f))
    }
}
