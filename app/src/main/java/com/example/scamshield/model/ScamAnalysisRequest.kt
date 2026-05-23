package com.example.scamshield.model

import com.google.gson.annotations.SerializedName

/**
 * Payload sent to the FastAPI POST /analyze endpoint.
 *
 * The backend expects a single "message" field containing the raw text to evaluate.
 * Serialised JSON: {"message": "<captured text>"}
 */
data class ScamAnalysisRequest(
    @SerializedName("message") val message: String,
)
