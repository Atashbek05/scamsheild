package com.example.scamshield.model

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("reply") val reply: String,
)
