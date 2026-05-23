package com.example.scamshield.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    val anyGranted = isNotificationGranted || isAccessibilityGranted || isOverlayGranted

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(stringResource(R.string.perm_title), color = CyberTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.perm_subtitle), color = CyberTextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        PermissionTile(
            icon = Icons.Filled.NotificationsActive,
            title = stringResource(R.string.perm_notification_title),
            description = stringResource(R.string.perm_notification_desc),
            granted = isNotificationGranted,
            onGrant = onGrantNotification,
        )
        Spacer(Modifier.height(12.dp))
        PermissionTile(
            icon = Icons.Filled.Accessibility,
            title = stringResource(R.string.perm_accessibility_title),
            description = stringResource(R.string.perm_accessibility_desc),
            granted = isAccessibilityGranted,
            onGrant = onGrantAccessibility,
        )
        Spacer(Modifier.height(12.dp))
        PermissionTile(
            icon = Icons.Filled.Layers,
            title = stringResource(R.string.perm_overlay_title),
            description = stringResource(R.string.perm_overlay_desc),
            granted = isOverlayGranted,
            onGrant = onGrantOverlay,
        )

        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onRefresh) {
                Text(stringResource(R.string.perm_refresh), color = CyberCyan)
            }
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (anyGranted) CyberGreen else CyberCyan,
                    contentColor = CyberBgDeep,
                ),
            ) {
                Text(
                    stringResource(if (allOptional) R.string.perm_skip else R.string.perm_enter),
                    fontWeight = FontWeight.Bold,
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
    CyberCard(accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(accent.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = CyberTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (granted) {
                        Spacer(Modifier.size(6.dp))
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(description, color = CyberTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(if (granted) R.string.perm_reopen else R.string.perm_grant),
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
