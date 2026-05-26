package com.example.scamshield.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.R
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.privacy_title),
                color         = CyberTextPrimary,
                fontSize      = 20.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
        }
        Text(
            stringResource(R.string.privacy_version),
            color    = CyberTextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 52.dp, bottom = 8.dp),
        )
        Spacer(Modifier.height(12.dp))

        PolicySection(
            title = stringResource(R.string.privacy_section_collected),
            body  = stringResource(R.string.privacy_collected_body),
        )
        PolicySection(
            title = stringResource(R.string.privacy_section_usage),
            body  = stringResource(R.string.privacy_usage_body),
        )
        PolicySection(
            title = stringResource(R.string.privacy_section_server),
            body  = stringResource(R.string.privacy_server_body),
        )
        PolicySection(
            title = stringResource(R.string.privacy_section_delete),
            body  = stringResource(R.string.privacy_delete_body),
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    CyberCard {
        Text(
            title,
            color         = CyberCyan,
            fontSize      = 12.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            body,
            color      = CyberTextSecondary,
            fontSize   = 13.sp,
            lineHeight = 20.sp,
        )
    }
    Spacer(Modifier.height(10.dp))
}
