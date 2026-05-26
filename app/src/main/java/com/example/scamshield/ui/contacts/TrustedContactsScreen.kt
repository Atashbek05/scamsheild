package com.example.scamshield.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamshield.R
import com.example.scamshield.ui.components.CyberCard
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import com.example.scamshield.ui.theme.CyberTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Fmt = SimpleDateFormat("MMM dd · HH:mm", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedContactsScreen(onBack: () -> Unit) {
    val vm           = viewModel<TrustedContactsViewModel>(factory = TrustedContactsViewModel.Factory)
    val items        by vm.trusted.collectAsStateWithLifecycle()
    var showAdd      by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(CyberBgDeep)) {
        ContactListHeader(
            title    = stringResource(R.string.trusted_title),
            subtitle = stringResource(R.string.trusted_count, items.size),
            accent   = CyberGreen,
            onBack   = onBack,
            onAdd    = { showAdd = true },
        )

        if (items.isEmpty()) {
            ContactEmptyState(
                icon    = Icons.Rounded.VerifiedUser,
                accent  = CyberGreen,
                message = stringResource(R.string.trusted_empty),
            )
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier            = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.phoneNumber }) { item ->
                    SwipeToDeleteItem(onDelete = { pendingDelete = item.phoneNumber }) {
                        CyberCard(accent = CyberGreen) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .background(CyberGreen.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.VerifiedUser, null, tint = CyberGreen, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.name.ifBlank { item.phoneNumber },
                                        color      = CyberTextPrimary,
                                        fontSize   = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (item.name.isNotBlank()) {
                                        Text(item.phoneNumber, color = CyberTextSecondary, fontSize = 12.sp)
                                    }
                                    Text(
                                        stringResource(R.string.trusted_added_at, Fmt.format(Date(item.addedAt))),
                                        color    = CyberTextMuted,
                                        fontSize = 10.sp,
                                    )
                                }
                                Box(
                                    Modifier
                                        .background(CyberGreen.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        stringResource(R.string.call_risk_trusted).uppercase(),
                                        color      = CyberGreen,
                                        fontSize   = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddTrustedSheet(
            onDismiss = { showAdd = false },
            onAdd     = { number, name ->
                vm.addContact(number, name)
                showAdd = false
            },
        )
    }

    pendingDelete?.let { number ->
        ContactDeleteDialog(
            title        = stringResource(R.string.trusted_delete_title),
            message      = stringResource(R.string.trusted_delete_message, number),
            confirmLabel = stringResource(R.string.trusted_delete_confirm),
            onConfirm    = { vm.removeContact(number); pendingDelete = null },
            onDismiss    = { pendingDelete = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTrustedSheet(
    onDismiss: () -> Unit,
    onAdd: (number: String, name: String) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var name   by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = CyberBgCard,
        dragHandle       = { SheetHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.trusted_add_title),
                color      = CyberTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
            )
            OutlinedTextField(
                value           = number,
                onValueChange   = { number = it },
                placeholder     = { Text(stringResource(R.string.trusted_add_number_hint), color = CyberTextMuted, fontSize = 13.sp) },
                leadingIcon     = { Icon(Icons.Rounded.Phone, null, tint = CyberCyan, modifier = Modifier.size(20.dp)) },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                colors          = sheetFieldColors(),
                shape           = RoundedCornerShape(12.dp),
                modifier        = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value           = name,
                onValueChange   = { name = it },
                placeholder     = { Text(stringResource(R.string.trusted_add_name_hint), color = CyberTextMuted, fontSize = 13.sp) },
                leadingIcon     = { Icon(Icons.Rounded.Person, null, tint = CyberTextSecondary, modifier = Modifier.size(20.dp)) },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors          = sheetFieldColors(),
                shape           = RoundedCornerShape(12.dp),
                modifier        = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.contacts_cancel), color = CyberTextSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick  = { onAdd(number, name) },
                    enabled  = number.isNotBlank(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = CyberGreen,
                        contentColor           = CyberBgDeep,
                        disabledContainerColor = CyberGreen.copy(alpha = 0.25f),
                        disabledContentColor   = CyberBgDeep.copy(alpha = 0.5f),
                    ),
                ) {
                    Text(stringResource(R.string.contacts_add), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(
        Modifier
            .padding(vertical = 10.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(CyberTextMuted.copy(alpha = 0.4f)),
    )
}
