package com.example.scamshield.network

import com.example.scamshield.model.ChatRequest
import com.example.scamshield.model.ChatResponse
import com.example.scamshield.model.ScamAnalysisRequest
import com.example.scamshield.model.ScamAnalysisResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ScamShieldApiService {

    @POST("v1/analyze")
    suspend fun analyzeText(@Body request: ScamAnalysisRequest): ScamAnalysisResponse

    @POST("v1/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
