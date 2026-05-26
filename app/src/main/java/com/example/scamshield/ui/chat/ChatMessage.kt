package com.example.scamshield.ui.chat

data class ChatMessage(
    val role: String,
    val text: String,
    val imageBase64: String? = null,
)
