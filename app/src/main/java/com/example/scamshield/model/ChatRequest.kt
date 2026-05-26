package com.example.scamshield.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("image_base64") val image_base64: String? = null,
)
