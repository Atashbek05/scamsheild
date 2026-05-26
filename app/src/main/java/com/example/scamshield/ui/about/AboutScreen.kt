package com.example.scamshield.ui.about

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.BuildConfig
import com.example.scamshield.R
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBorderSubtle
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Top bar ──────────────────────────────────────────────
        Row(
            Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, null, tint = CyberCyan, modifier = Modifier.size(22.dp))
            }
            Text(
                stringResource(R.string.about_title),
                color         = CyberTextPrimary,
                fontSize      = 20.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
        }

        // ── Logo + name ───────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(108.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(CyberCyan.copy(alpha = 0.22f), CyberCyan.copy(alpha = 0f))
                        ),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(76.dp)
                        .background(CyberCyan.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Shield, null, tint = CyberCyan, modifier = Modifier.size(42.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "ScamShield",
                color         = CyberTextPrimary,
                fontSize      = 26.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "v${BuildConfig.VERSION_NAME}",
                color    = CyberCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.about_app_desc),
                color      = CyberTextSecondary,
                fontSize   = 14.sp,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.padding(horizontal = 32.dp),
            )
        }

        // ── Contacts ──────────────────────────────────────────────
        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionHeader(stringResource(R.string.about_section_contacts))
            Spacer(Modifier.height(8.dp))
            CyberCard {
                AboutRow(
                    icon    = Icons.Rounded.Email,
                    label   = stringResource(R.string.about_support_label),
                    value   = stringResource(R.string.about_support_email),
                    accent  = CyberGreen,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO,
                            Uri.parse("mailto:${context.getString(R.string.about_support_email)}"))
                        context.startActivity(intent)
                    },
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Documents ─────────────────────────────────────────
            SectionHeader(stringResource(R.string.about_section_documents))
            Spacer(Modifier.height(8.dp))
            CyberCard {
                AboutRow(
                    icon    = Icons.Rounded.Shield,
                    label   = stringResource(R.string.settings_privacy_policy),
                    accent  = CyberCyan,
                    onClick = onOpenPrivacyPolicy,
                )
                HorizontalDivider(
                    Modifier.padding(vertical = 2.dp),
                    color     = CyberBorderSubtle,
                    thickness = 0.5.dp,
                )
                AboutRow(
                    icon    = Icons.Rounded.Gavel,
                    label   = stringResource(R.string.about_terms),
                    accent  = CyberCyan,
                    onClick = onOpenPrivacyPolicy,
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Copyright ─────────────────────────────────────────
            Text(
                stringResource(R.string.about_copyright),
                color     = CyberTextMuted,
                fontSize  = 11.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color         = CyberTextMuted,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Medium,
        letterSpacing = 1.sp,
        modifier      = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    label: String,
    accent: androidx.compose.ui.graphics.Color = CyberCyan,
    value: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(accent.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (value != null) {
                Text(value, color = accent, fontSize = 12.sp)
            }
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = CyberTextMuted, modifier = Modifier.size(18.dp))
    }
}
