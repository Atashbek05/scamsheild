package com.example.scamshield.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.R
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary

@Composable
fun PermissionsWizardScreen(
    isNotificationGranted: Boolean,
    isAccessibilityGranted: Boolean,
    isOverlayGranted: Boolean,
    onGrantNotification: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onGrantOverlay: () -> Unit,
    onRefresh: () -> Unit,
    onContinue: () -> Unit,
) {
    val allOptional = !isNotificationGranted && !isAccessibilityGranted && !isOverlayGranted
    val anyGranted  = isNotificationGranted || isAccessibilityGranted || isOverlayGranted
    val grantedCount = listOf(isNotificationGranted, isAccessibilityGranted, isOverlayGranted).count { it }

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            stringResource(R.string.perm_title),
            color         = CyberTextPrimary,
            fontSize      = 22.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.perm_subtitle),
            color    = CyberTextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(6.dp))

        // Progress indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { i ->
                val granted = i < grantedCount
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (granted) CyberGreen else CyberTextSecondary.copy(alpha = 0.2f)
                        ),
                )
                if (i < 2) Spacer(Modifier.size(4.dp))
            }
        }
        Spacer(Modifier.height(20.dp))

        PermissionTile(
            icon        = Icons.Rounded.NotificationsActive,
            title       = stringResource(R.string.perm_notification_title),
            description = stringResource(R.string.perm_notification_desc),
            granted     = isNotificationGranted,
            onGrant     = onGrantNotification,
        )
        Spacer(Modifier.height(10.dp))
        PermissionTile(
            icon        = Icons.Rounded.Accessibility,
            title       = stringResource(R.string.perm_accessibility_title),
            description = stringResource(R.string.perm_accessibility_desc),
            granted     = isAccessibilityGranted,
            onGrant     = onGrantAccessibility,
        )
        Spacer(Modifier.height(10.dp))
        PermissionTile(
            icon        = Icons.Rounded.Layers,
            title       = stringResource(R.string.perm_overlay_title),
            description = stringResource(R.string.perm_overlay_desc),
            granted     = isOverlayGranted,
            onGrant     = onGrantOverlay,
        )

        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onRefresh) {
                Text(stringResource(R.string.perm_refresh), color = CyberCyan)
            }
            Button(
                onClick = onContinue,
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (anyGranted) CyberGreen else CyberCyan,
                    contentColor   = CyberBgDeep,
                ),
            ) {
                Text(
                    stringResource(if (allOptional) R.string.perm_skip else R.string.perm_enter),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun PermissionTile(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    val accent = if (granted) CyberGreen else CyberCyan
    CyberCard(accent = accent, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        color      = CyberTextPrimary,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.weight(1f),
                    )
                    if (granted) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            null,
                            tint     = CyberGreen,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    description,
                    color      = CyberTextSecondary,
                    fontSize   = 12.sp,
                    lineHeight = 17.sp,
                    maxLines   = 3,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick  = onGrant,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                ) {
                    Text(
                        stringResource(if (granted) R.string.perm_reopen else R.string.perm_grant),
                        color      = accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                    )
                }
            }
        }
    }
}
