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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamshield.ui.theme.CyberBgCard
import com.example.scamshield.ui.theme.CyberBgDeep
import com.example.scamshield.ui.theme.CyberBgSurface
import com.example.scamshield.ui.theme.CyberCyan
import com.example.scamshield.ui.theme.CyberTextMuted
import com.example.scamshield.ui.theme.CyberTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AiChatScreen() {
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory)
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var pendingImageBase64 by remember { mutableStateOf<String?>(null) }
    var pendingImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
        inputText = ""
        pendingImageBase64 = null
        pendingImageBitmap = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDeep)
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberBgSurface)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "AI CHAT",
                color = CyberCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
                            modifier = Modifier.size(24.dp),
                            color = CyberCyan,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }

        if (pendingImageBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberBgSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Image(
                    bitmap = pendingImageBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                IconButton(
                    onClick = {
                        pendingImageBase64 = null
                        pendingImageBitmap = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 58.dp, y = (-4).dp)
                        .size(24.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Remove image",
                        tint = CyberTextPrimary,
                        modifier = Modifier
                            .background(CyberBgCard, RoundedCornerShape(50))
                            .padding(2.dp)
                            .size(16.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberBgSurface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                enabled = !isLoading,
            ) {
                Icon(
                    Icons.Rounded.AttachFile,
                    contentDescription = "Attach image",
                    tint = if (!isLoading) CyberCyan else CyberTextMuted,
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Введите сообщение…",
                        color = CyberTextMuted,
                        fontSize = 14.sp,
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CyberTextMuted.copy(alpha = 0.3f),
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextPrimary,
                    cursorColor = CyberCyan,
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() || pendingImageBase64 != null) sendCurrent()
                    },
                ),
            )

            val canSend = (inputText.isNotBlank() || pendingImageBase64 != null) && !isLoading
            IconButton(
                onClick = ::sendCurrent,
                enabled = canSend,
            ) {
                Icon(
                    Icons.Rounded.Send,
                    contentDescription = "Send",
                    tint = if (canSend) CyberCyan else CyberTextMuted,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isUser) CyberCyan.copy(alpha = 0.12f) else CyberBgCard,
                    shape = bubbleShape,
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) CyberCyan.copy(alpha = 0.4f) else CyberTextMuted.copy(alpha = 0.2f),
                    shape = bubbleShape,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (message.imageBase64 != null) {
                val bitmap = remember(message.imageBase64) {
                    try {
                        val bytes = Base64.decode(message.imageBase64, Base64.NO_WRAP)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (_: Exception) {
                        null
                    }
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    if (message.text.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    color = if (isUser) CyberCyan else CyberTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}
