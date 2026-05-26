package com.example.scamshield.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.scamshield.R
import com.example.scamshield.model.ChatRequest
import com.example.scamshield.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(text: String, imageBase64: String? = null) {
        if (text.isBlank() && imageBase64 == null) return

        _messages.update { it + ChatMessage(role = "user", text = text, imageBase64 = imageBase64) }
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.chat(ChatRequest(message = text, image_base64 = imageBase64))
                }
                _messages.update { it + ChatMessage(role = "ai", text = response.reply) }
            } catch (e: Exception) {
                _messages.update {
                    it + ChatMessage(role = "ai", text = app.getString(R.string.chat_error_connection))
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ChatViewModel(checkNotNull(this[APPLICATION_KEY]))
            }
        }
    }
}
