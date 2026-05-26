package com.example.scamshield.ui.contacts

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamshield.R
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberGlassRed
import com.example.scamshield.ui.theme.CyberRed
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary

@Composable
internal fun ContactListHeader(
    title: String,
    subtitle: String,
    accent: Color,
    onBack: () -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CyberBgDeep)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, null, tint = CyberTextPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color         = CyberTextPrimary,
                fontWeight    = FontWeight.Bold,
                fontSize      = 18.sp,
                letterSpacing = 0.sp,
            )
            Text(subtitle, color = CyberTextSecondary, fontSize = 12.sp)
        }
        IconButton(onClick = onAdd) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun ContactEmptyState(
    icon: ImageVector,
    accent: Color,
    message: String,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(88.dp)
                .background(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.15f), Color.Transparent)),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            message,
            color     = CyberTextSecondary,
            fontSize  = 14.sp,
            lineHeight = 21.sp,
            textAlign  = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeToDeleteItem(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            false // always spring back — deletion confirmed via dialog
        },
    )
    SwipeToDismissBox(
        state                      = state,
        enableDismissFromStartToEnd = false,
        backgroundContent          = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberGlassRed),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    Modifier.padding(end = 20.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.DeleteForever, null, tint = CyberRed, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.history_delete),
                        color = CyberRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    ) {
        content()
    }
}

@Composable
internal fun ContactDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest  = onDismiss,
        title             = { Text(title, color = CyberTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text              = { Text(message, color = CyberTextSecondary, fontSize = 13.sp, lineHeight = 18.sp) },
        confirmButton     = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = CyberRed, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton     = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.contacts_cancel), color = CyberTextSecondary)
            }
        },
        containerColor    = CyberBgCard,
    )
}

@Composable
internal fun sheetFieldColors() = androidx.compose.material3.TextFieldDefaults.colors(
    focusedContainerColor     = CyberBgCard,
    unfocusedContainerColor   = CyberBgCard,
    focusedTextColor          = CyberTextPrimary,
    unfocusedTextColor        = CyberTextPrimary,
    cursorColor               = com.example.scamshield.ui.theme.CyberCyan,
    focusedIndicatorColor     = com.example.scamshield.ui.theme.CyberCyan,
    unfocusedIndicatorColor   = CyberBgSurface,
    focusedLeadingIconColor   = com.example.scamshield.ui.theme.CyberCyan,
    unfocusedLeadingIconColor = CyberTextMuted,
)
