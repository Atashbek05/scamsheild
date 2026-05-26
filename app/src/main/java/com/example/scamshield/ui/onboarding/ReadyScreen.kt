package com.example.scamshield.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.R
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.theme.CyberAmber
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary

@Composable
fun ReadyScreen(
    isNotificationGranted: Boolean,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    onGrantNotification: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onGrantOverlay: () -> Unit,
    onRefresh: () -> Unit,
    onStart: () -> Unit,
) {
    val allGranted = isNotificationGranted && isAccessibilityGranted && isOverlayGranted
    val accent     = if (allGranted) CyberGreen else CyberAmber

    val infinite = rememberInfiniteTransition(label = "shield_ring")
    val sweep by infinite.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing)),
        label         = "sweep",
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Animated shield ring
        Box(Modifier.size(148.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(148.dp)) {
                val s = size.minDimension
                val r = s / 2f - 8f
                drawCircle(accent.copy(alpha = 0.07f), r)
                drawArc(
                    color      = accent,
                    startAngle = sweep,
                    sweepAngle = 120f,
                    useCenter  = false,
                    style      = Stroke(width = 4.5f, cap = StrokeCap.Round),
                    topLeft    = Offset(8f, 8f),
                    size       = Size(s - 16f, s - 16f),
                )
                drawArc(
                    color      = accent.copy(alpha = 0.28f),
                    startAngle = sweep + 180f,
                    sweepAngle = 70f,
                    useCenter  = false,
                    style      = Stroke(width = 2.5f, cap = StrokeCap.Round),
                    topLeft    = Offset(8f, 8f),
                    size       = Size(s - 16f, s - 16f),
                )
                drawCircle(accent.copy(alpha = 0.10f), r * 0.60f, style = Stroke(1.5f))
            }
            Box(
                Modifier
                    .size(88.dp)
                    .background(accent.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Shield, null, tint = accent, modifier = Modifier.size(46.dp))
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            stringResource(if (allGranted) R.string.ready_title_protected else R.string.perm_title),
            color         = accent,
            fontSize      = 24.sp,
            fontWeight    = FontWeight.Bold,
            textAlign     = TextAlign.Center,
            letterSpacing = 0.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(if (allGranted) R.string.ready_desc_protected else R.string.ready_desc_partial),
            color      = CyberTextSecondary,
            fontSize   = 14.sp,
            lineHeight = 21.sp,
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        CyberCard(accent = accent) {
            Text(
                stringResource(R.string.ready_services_title),
                color         = CyberTextMuted,
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(14.dp))
            ServiceRow(
                icon    = Icons.Rounded.NotificationsActive,
                label   = stringResource(R.string.perm_notification_title),
                granted = isNotificationGranted,
                onGrant = onGrantNotification,
            )
            Spacer(Modifier.height(12.dp))
            ServiceRow(
                icon    = Icons.Rounded.Accessibility,
                label   = stringResource(R.string.perm_accessibility_title),
                granted = isAccessibilityGranted,
                onGrant = onGrantAccessibility,
            )
            Spacer(Modifier.height(12.dp))
            ServiceRow(
                icon    = Icons.Rounded.Layers,
                label   = stringResource(R.string.perm_overlay_title),
                granted = isOverlayGranted,
                onGrant = onGrantOverlay,
            )
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, null, tint = CyberCyan, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.perm_refresh), color = CyberCyan, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))

        Button(
            onClick  = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor   = CyberBgDeep,
            ),
        ) {
            Text(
                stringResource(R.string.onboarding_get_started),
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp,
            )
        }
    }
}

@Composable
private fun ServiceRow(
    icon: ImageVector,
    label: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    val color = if (granted) CyberGreen else CyberRed
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color      = CyberTextPrimary,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.weight(1f),
        )
        if (granted) {
            Icon(Icons.Rounded.CheckCircle, null, tint = CyberGreen, modifier = Modifier.size(20.dp))
        } else {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberRed.copy(alpha = 0.10f))
                    .clickable(onClick = onGrant)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    stringResource(R.string.perm_grant),
                    color      = CyberRed,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
