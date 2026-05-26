package com.example.scamshield.network

import com.example.scamshield.model.ScamAnalysisRequest
import com.example.scamshield.model.ScamAnalysisResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the ScamShield FastAPI backend.
 *
 * All functions are suspend — callers must run them from a coroutine scope.
 * Retrofit handles the IO dispatch internally when used with its coroutines
 * adapter; we still schedule calls on Dispatchers.IO at the call site to keep
 * the intent explicit.
 */
interface ScamShieldApiService {

    /**
     * POST /v1/analyze
     *
     * Sends captured text to the backend and receives a scam analysis result.
     * Throws [retrofit2.HttpException] on non-2xx responses and
     * [java.io.IOException] on network failures — both are caught by the repository.
     */
    @POST("v1/analyze")
    suspend fun analyzeText(@Body request: ScamAnalysisRequest): ScamAnalysisResponse
}
