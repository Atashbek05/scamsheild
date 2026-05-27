package com.example.scamshield.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamshield.ui.components.NeonPulse
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberBorder
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberGreen
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AiChatScreen() {
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory)
    val messages  by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var inputText          by remember { mutableStateOf("") }
    var pendingImageBase64 by remember { mutableStateOf<String?>(null) }
    var pendingImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val itemCount = messages.size + if (isLoading) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(selectedUri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val bmp    = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        withContext(Dispatchers.Main) {
                            pendingImageBase64 = base64
                            pendingImageBitmap = bmp
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun sendCurrent() {
        viewModel.sendMessage(inputText.trim(), pendingImageBase64)
        inputText          = ""
        pendingImageBase64 = null
        pendingImageBitmap = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .imePadding(),
    ) {
        // ── Premium header ────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.07f),
                            Color.Transparent,
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Chat, null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text          = "AI ASSISTANT",
                        color         = CyberTextPrimary,
                        fontSize      = 18.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        text     = "ScamShield AI",
                        color    = CyberTextMuted,
                        fontSize = 11.sp,
                    )
                }
                NeonPulse(color = if (isLoading) CyberCyan else CyberGreen, diameter = 5.dp)
            }
        }

        // ── Message list ──────────────────────────────────────────────────────
        LazyColumn(
            state            = listState,
            modifier         = Modifier.weight(1f),
            contentPadding   = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(24.dp),
                            color       = CyberCyan,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }

        // ── Pending image preview ─────────────────────────────────────────────
        if (pendingImageBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBgSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Image(
                    bitmap             = pendingImageBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier           = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
                IconButton(
                    onClick  = { pendingImageBase64 = null; pendingImageBitmap = null },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 58.dp, y = (-4).dp)
                        .size(24.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove image",
                        tint     = CyberTextPrimary,
                        modifier = Modifier
                            .background(CyberBgCard, RoundedCornerShape(50))
                            .padding(2.dp)
                            .size(16.dp),
                    )
                }
            }
        }

        // ── Premium input bar ─────────────────────────────────────────────────
        val borderColor = CyberBorder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberBgSurface)
                .drawBehind {
                    drawLine(
                        color       = borderColor,
                        start       = Offset(0f, 0f),
                        end         = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBgCard)
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    .clickable(enabled = !isLoading) { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.AttachFile,
                    contentDescription = "Attach image",
                    tint               = if (!isLoading) CyberCyan else CyberTextMuted,
                    modifier           = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            OutlinedTextField(
                value         = inputText,
                onValueChange = { inputText = it },
                modifier      = Modifier.weight(1f),
                placeholder   = {
                    Text(
                        text     = "Введите сообщение…",
                        color    = CyberTextMuted,
                        fontSize = 14.sp,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = CyberCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor     = CyberTextPrimary,
                    unfocusedTextColor   = CyberTextPrimary,
                    cursorColor          = CyberCyan,
                    focusedContainerColor   = CyberBgCard,
                    unfocusedContainerColor = CyberBgCard,
                ),
                shape       = RoundedCornerShape(16.dp),
                maxLines    = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() || pendingImageBase64 != null) sendCurrent()
                    },
                ),
            )

            Spacer(Modifier.width(8.dp))

            val canSend = (inputText.isNotBlank() || pendingImageBase64 != null) && !isLoading
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (canSend) CyberCyan.copy(alpha = 0.18f) else CyberBgCard
                    )
                    .border(
                        1.dp,
                        if (canSend) CyberCyan.copy(alpha = 0.5f) else CyberBorder,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable(enabled = canSend, onClick = ::sendCurrent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Send,
                    contentDescription = "Send",
                    tint               = if (canSend) CyberCyan else CyberTextMuted,
                    modifier           = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser    = message.role == "user"
    val bubbleShape = RoundedCornerShape(
        topStart    = 18.dp,
        topEnd      = 18.dp,
        bottomStart = if (isUser) 18.dp else 4.dp,
        bottomEnd   = if (isUser) 4.dp else 18.dp,
    )

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isUser) CyberCyan.copy(alpha = 0.14f) else CyberBgCard,
                    shape = bubbleShape,
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) CyberCyan.copy(alpha = 0.45f) else CyberBorder,
                    shape = bubbleShape,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (message.imageBase64 != null) {
                val bitmap = remember(message.imageBase64) {
                    try {
                        val bytes = Base64.decode(message.imageBase64, Base64.NO_WRAP)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (_: Exception) { null }
                }
                bitmap?.let {
                    Image(
                        bitmap             = it.asImageBitmap(),
                        contentDescription = null,
                        modifier           = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    if (message.text.isNotBlank()) Spacer(Modifier.height(6.dp))
                }
            }
            if (message.text.isNotBlank()) {
                Text(
                    text       = message.text,
                    color      = if (isUser) CyberCyan else CyberTextPrimary,
                    fontSize   = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}
