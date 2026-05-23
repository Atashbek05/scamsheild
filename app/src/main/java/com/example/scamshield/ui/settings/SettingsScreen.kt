package com.example.scamshield.ui.settings

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamshield.R
import com.example.scamshield.data.settings.DarkMode
import com.example.scamshield.data.settings.OverlayPosition
import com.example.scamshield.data.settings.ScanMode
import com.example.scamshield.data.settings.Sensitivity
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary

@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings by vm.settings.collectAsStateWithLifecycle()
    var backendDraft by remember(settings.backendUrl) { mutableStateOf(settings.backendUrl) }

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(stringResource(R.string.settings_title), color = CyberTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        Spacer(Modifier.height(14.dp))

        SettingCard(icon = Icons.Filled.Shield, title = stringResource(R.string.settings_active_protection)) {
            ToggleRow(
                label = stringResource(R.string.settings_realtime_monitoring),
                description = stringResource(R.string.settings_realtime_monitoring_desc),
                value = settings.activeProtection,
                onChange = vm::setActiveProtection,
            )
            ToggleRow(
                label = stringResource(R.string.settings_auto_scan),
                description = stringResource(R.string.settings_auto_scan_desc),
                value = settings.autoScanEnabled,
                onChange = vm::setAutoScanEnabled,
            )
        }
        Spacer(Modifier.height(14.dp))

        SettingCard(icon = Icons.Filled.Tune, title = stringResource(R.string.settings_sensitivity)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sensitivity.entries.forEach { s ->
                    ChoiceChip(stringResource(s.labelRes), settings.sensitivity == s) { vm.setSensitivity(s) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.settings_sensitivity_info,
                    (settings.sensitivity.overlayThreshold * 100).toInt(),
                    (settings.sensitivity.notifyThreshold * 100).toInt(),
                ),
                color = CyberTextSecondary, fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(14.dp))

        SettingCard(icon = Icons.Filled.Psychology, title = stringResource(R.string.settings_scan_mode)) {
            ScanMode.entries.forEach { mode ->
                RadioRow(
                    selected = settings.scanMode == mode,
                    title = stringResource(mode.labelRes),
                    description = stringResource(mode.descriptionRes),
                    onClick = { vm.setScanMode(mode) },
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        SettingCard(icon = Icons.Filled.Notifications, title = stringResource(R.string.settings_notifications)) {
            ToggleRow(stringResource(R.string.settings_notif_enable), stringResource(R.string.settings_notif_enable_desc), settings.notificationsEnabled, vm::setNotificationsEnabled)
            ToggleRow(stringResource(R.string.settings_notif_sound), stringResource(R.string.settings_notif_sound_desc), settings.notificationSound, vm::setNotificationSound)
            ToggleRow(stringResource(R.string.settings_notif_vibrate), stringResource(R.string.settings_notif_vibrate_desc), settings.notificationVibrate, vm::setNotificationVibrate)
        }
        Spacer(Modifier.height(14.dp))

        SettingCard(icon = Icons.Filled.WarningAmber, title = stringResource(R.string.settings_overlay_warnings)) {
            ToggleRow(stringResource(R.string.settings_overlay_enable), stringResource(R.string.settings_overlay_enable_desc), settings.overlayEnabled, vm::setOverlayEnabled)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.settings_overlay_auto_dismiss, settings.overlayAutoDismissSec), color = CyberTextSecondary, fontSize = 11.sp)
            Slider(
                value = settings.overlayAutoDismissSec.toFloat(),
                onValueChange = { vm.setOverlayAutoDismiss(it.toInt()) },
                valueRange = 3f..15f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = CyberCyan,
                    activeTrackColor = CyberCyan,
                    inactiveTrackColor = CyberBgSurface,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayPosition.entries.forEach { pos ->
                    ChoiceChip(stringResource(pos.labelRes), settings.overlayPosition == pos) { vm.setOverlayPosition(pos) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        SettingCard(icon = Icons.Filled.DarkMode, title = stringResource(R.string.settings_appearance)) {
            DarkMode.entries.forEach { m ->
                RadioRow(
                    selected = settings.darkMode == m,
                    title = when (m) {
                        DarkMode.SYSTEM            -> stringResource(R.string.settings_darkmode_system_title)
                        DarkMode.DARK              -> stringResource(R.string.settings_darkmode_dark_title)
                        DarkMode.ALWAYS_DARK_CYBER -> stringResource(R.string.settings_darkmode_cyber_title)
                    },
                    description = when (m) {
                        DarkMode.SYSTEM            -> stringResource(R.string.settings_darkmode_system_desc)
                        DarkMode.DARK              -> stringResource(R.string.settings_darkmode_dark_desc)
                        DarkMode.ALWAYS_DARK_CYBER -> stringResource(R.string.settings_darkmode_cyber_desc)
                    },
                    onClick = { vm.setDarkMode(m) },
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        SettingCard(icon = Icons.Filled.Cloud, title = stringResource(R.string.settings_backend)) {
            OutlinedTextField(
                value = backendDraft,
                onValueChange = { backendDraft = it },
                label = { Text(stringResource(R.string.settings_backend_url), color = CyberTextSecondary, fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .clickable { vm.setBackendUrl(backendDraft) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(stringResource(R.string.settings_save), color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        SettingCard(icon = Icons.Filled.Bolt, title = stringResource(R.string.settings_about)) {
            Text(stringResource(R.string.settings_about_version), color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.settings_about_desc), color = CyberTextSecondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SettingCard(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                title,
                color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun ToggleRow(label: String, description: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = CyberTextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberGreen,
                checkedTrackColor = CyberGreen.copy(alpha = 0.35f),
                uncheckedThumbColor = CyberTextSecondary,
                uncheckedTrackColor = CyberBgCard,
            ),
        )
    }
}

@Composable
private fun RadioRow(selected: Boolean, title: String, description: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (selected) CyberCyan else CyberBgCard),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(8.dp).background(CyberBgDeep, androidx.compose.foundation.shape.CircleShape))
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = CyberTextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor   = CyberBgCard,
    unfocusedContainerColor = CyberBgCard,
    focusedTextColor        = CyberTextPrimary,
    unfocusedTextColor      = CyberTextPrimary,
    cursorColor             = CyberCyan,
    focusedIndicatorColor   = CyberCyan,
    unfocusedIndicatorColor = CyberBgSurface,
)
