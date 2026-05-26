package com.example.scamshield.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamshield.R
import com.example.scamshield.data.settings.DarkMode
import com.example.scamshield.data.settings.HistoryRetention
import com.example.scamshield.data.settings.OverlayPosition
import com.example.scamshield.data.settings.ScanMode
import com.example.scamshield.data.settings.Sensitivity
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary

@Composable
fun SettingsScreen(
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenBlockedNumbers: () -> Unit = {},
    onOpenTrustedContacts: () -> Unit = {},
    onCheckForUpdates: (() -> Unit)? = null,
) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings by vm.settings.collectAsStateWithLifecycle()
    var backendDraft by remember(settings.backendUrl) { mutableStateOf(settings.backendUrl) }
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            color         = CyberTextPrimary,
            fontSize      = 20.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.sp,
        )
        Spacer(Modifier.height(16.dp))

        SettingSection(icon = Icons.Rounded.Shield, title = stringResource(R.string.settings_active_protection)) {
            ToggleRow(
                label       = stringResource(R.string.settings_realtime_monitoring),
                description = stringResource(R.string.settings_realtime_monitoring_desc),
                value       = settings.activeProtection,
                onChange    = vm::setActiveProtection,
            )
            ToggleRow(
                label       = stringResource(R.string.settings_auto_scan),
                description = stringResource(R.string.settings_auto_scan_desc),
                value       = settings.autoScanEnabled,
                onChange    = vm::setAutoScanEnabled,
            )
        }

        SettingSection(icon = Icons.Rounded.Tune, title = stringResource(R.string.settings_sensitivity)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                color    = CyberTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }

        SettingSection(icon = Icons.Rounded.Psychology, title = stringResource(R.string.settings_scan_mode)) {
            ScanMode.entries.forEach { mode ->
                RadioRow(
                    selected    = settings.scanMode == mode,
                    title       = stringResource(mode.labelRes),
                    description = stringResource(mode.descriptionRes),
                    onClick     = { vm.setScanMode(mode) },
                )
            }
        }

        SettingSection(icon = Icons.Rounded.Notifications, title = stringResource(R.string.settings_notifications)) {
            ToggleRow(stringResource(R.string.settings_notif_enable),     stringResource(R.string.settings_notif_enable_desc),     settings.notificationsEnabled, vm::setNotificationsEnabled)
            ToggleRow(stringResource(R.string.settings_notif_sound),      stringResource(R.string.settings_notif_sound_desc),      settings.notificationSound,    vm::setNotificationSound)
            ToggleRow(stringResource(R.string.settings_notif_vibrate),    stringResource(R.string.settings_notif_vibrate_desc),    settings.notificationVibrate,  vm::setNotificationVibrate)
            ToggleRow(stringResource(R.string.settings_vibrate_threats),  stringResource(R.string.settings_vibrate_threats_desc),  settings.vibrationEnabled,     vm::setVibrationEnabled)
            ToggleRow(stringResource(R.string.settings_sound_critical),   stringResource(R.string.settings_sound_critical_desc),   settings.soundOnCritical,      vm::setSoundOnCritical)
        }

        SettingSection(icon = Icons.Rounded.Warning, title = stringResource(R.string.settings_overlay_warnings)) {
            ToggleRow(stringResource(R.string.settings_overlay_enable), stringResource(R.string.settings_overlay_enable_desc), settings.overlayEnabled, vm::setOverlayEnabled)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_overlay_auto_dismiss, settings.overlayAutoDismissSec),
                color    = CyberTextSecondary,
                fontSize = 11.sp,
            )
            Slider(
                value         = settings.overlayAutoDismissSec.toFloat(),
                onValueChange = { vm.setOverlayAutoDismiss(it.toInt()) },
                valueRange    = 3f..15f,
                steps         = 11,
                colors        = SliderDefaults.colors(
                    thumbColor        = CyberCyan,
                    activeTrackColor  = CyberCyan,
                    inactiveTrackColor = CyberBgSurface,
                ),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OverlayPosition.entries.forEach { pos ->
                    ChoiceChip(stringResource(pos.labelRes), settings.overlayPosition == pos) { vm.setOverlayPosition(pos) }
                }
            }
        }

        SettingSection(icon = Icons.Rounded.DarkMode, title = stringResource(R.string.settings_appearance)) {
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
            Spacer(Modifier.height(4.dp))
            NavRow(
                icon        = Icons.Rounded.Language,
                iconTint    = CyberCyan,
                label       = stringResource(R.string.settings_language),
                description = stringResource(R.string.settings_language_desc),
                onClick     = {
                    context.startActivity(
                        Intent(Settings.ACTION_LOCALE_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
            )
        }

        SettingSection(icon = Icons.Rounded.Cloud, title = stringResource(R.string.settings_backend)) {
            OutlinedTextField(
                value         = backendDraft,
                onValueChange = { backendDraft = it },
                label         = { Text(stringResource(R.string.settings_backend_url), color = CyberTextSecondary, fontSize = 11.sp) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                colors        = settingsFieldColors(),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCyan.copy(alpha = 0.12f))
                        .clickable { vm.setBackendUrl(backendDraft) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_save), color = CyberCyan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }

        SettingSection(icon = Icons.Rounded.History, title = stringResource(R.string.settings_data)) {
            Text(
                stringResource(R.string.settings_history_retention),
                color      = CyberTextPrimary,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryRetention.entries.forEach { retention ->
                    ChoiceChip(
                        label    = stringResource(retention.labelRes),
                        selected = settings.historyRetention == retention,
                        onClick  = { vm.setHistoryRetention(retention) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (settings.historyRetention == HistoryRetention.FOREVER)
                    stringResource(settings.historyRetention.descRes)
                else
                    stringResource(settings.historyRetention.descRes, settings.historyRetention.days),
                color    = CyberTextSecondary,
                fontSize = 11.sp,
            )
        }

        SettingSection(icon = Icons.Rounded.Phone, title = stringResource(R.string.settings_call_lists)) {
            NavRow(
                icon        = Icons.Rounded.Block,
                iconTint    = CyberRed,
                label       = stringResource(R.string.settings_blocked_numbers),
                description = stringResource(R.string.settings_blocked_numbers_desc),
                onClick     = onOpenBlockedNumbers,
            )
            Spacer(Modifier.height(2.dp))
            NavRow(
                icon        = Icons.Rounded.VerifiedUser,
                iconTint    = CyberGreen,
                label       = stringResource(R.string.settings_trusted_contacts),
                description = stringResource(R.string.settings_trusted_contacts_desc),
                onClick     = onOpenTrustedContacts,
            )
        }

        SettingSection(icon = Icons.Rounded.Bolt, title = stringResource(R.string.settings_about)) {
            ToggleRow(
                label       = stringResource(R.string.settings_crash_reporting),
                description = stringResource(R.string.settings_crash_reporting_desc),
                value       = settings.crashReportingEnabled,
                onChange    = vm::setCrashReportingEnabled,
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.settings_about_version), color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.settings_about_desc), color = CyberTextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            if (onCheckForUpdates != null) {
                Spacer(Modifier.height(2.dp))
                NavRow(
                    icon        = Icons.Rounded.SystemUpdate,
                    iconTint    = CyberCyan,
                    label       = stringResource(R.string.update_check_settings_title),
                    description = stringResource(R.string.update_check_settings_desc),
                    onClick     = onCheckForUpdates,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenPrivacyPolicy)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(CyberCyan.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Shield, null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_privacy_policy), color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.settings_privacy_policy_desc), color = CyberTextSecondary, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenAbout)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(CyberCyan.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Info, null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_about_open), color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.settings_about_open_desc), color = CyberTextSecondary, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SettingSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    CyberCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(CyberCyan.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = CyberCyan, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.size(10.dp))
            Text(
                title,
                color         = CyberTextPrimary,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 14.sp,
                letterSpacing = 0.sp,
                modifier      = Modifier.weight(1f),
                maxLines      = 1,
                overflow      = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ToggleRow(label: String, description: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, color = CyberTextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.size(12.dp))
        Switch(
            checked        = value,
            onCheckedChange = onChange,
            colors         = SwitchDefaults.colors(
                checkedThumbColor    = CyberBgDeep,
                checkedTrackColor    = CyberGreen,
                uncheckedThumbColor  = CyberTextSecondary,
                uncheckedTrackColor  = CyberBgCard,
                uncheckedBorderColor = CyberTextMuted,
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
                .clip(CircleShape)
                .background(if (selected) CyberCyan else CyberBgCard)
                .border(1.5.dp, if (selected) CyberCyan else CyberTextMuted.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(7.dp).background(CyberBgDeep, CircleShape))
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, color = CyberTextSecondary, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) CyberCyan.copy(alpha = 0.15f) else CyberBgCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
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
private fun NavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .background(iconTint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(description, color = CyberTextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun settingsFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor   = CyberBgCard,
    unfocusedContainerColor = CyberBgCard,
    focusedTextColor        = CyberTextPrimary,
    unfocusedTextColor      = CyberTextPrimary,
    cursorColor             = CyberCyan,
    focusedIndicatorColor   = CyberCyan,
    unfocusedIndicatorColor = CyberBgSurface,
    focusedLabelColor       = CyberCyan,
    unfocusedLabelColor     = CyberTextSecondary,
)
