package com.example.scamshield.repository

import android.util.Log
import com.example.scamshield.util.logD
import com.example.scamshield.model.ScamAnalysisRequest
import com.example.scamshield.model.ScamAnalysisResponse
import com.example.scamshield.network.ApiClient
import retrofit2.HttpException
import java.io.IOException

/**
 * Single source of truth for scam-analysis data.
 *
 * Wraps every Retrofit call in [Result] so callers never handle raw exceptions —
 * they only pattern-match on success/failure. All request/response logging and
 * error categorisation live here so service-layer call sites stay clean.
 *
 * Request format:  POST /analyze  →  {"message": "<text>"}
 * Response format: {"scam_probability": 0.0–1.0, "label": "scam"|"safe",
 *                   "suspicious_keywords": [...]}
 *
 * Error categories and their root causes:
 *   HttpException  — backend returned 4xx/5xx (bad request, model error, …)
 *   IOException    — network failure: no connectivity, DNS, timeout, connection reset
 *                    (includes Render.com cold-start timeouts on the first daily request)
 *   Exception      — Gson deserialisation failure or unexpected runtime error
 */
class BackendUnavailableException(cause: Throwable) : Exception("Backend unavailable", cause)

class ScamAnalysisRepository {

    companion object {
        private const val TAG = "ScamAnalysisRepo"

        // Number of characters to include in the outgoing-request log preview.
        // Long enough to identify the content; short enough to keep logs readable.
        private const val LOG_PREVIEW_LEN = 80
    }

    private val api = ApiClient.apiService

    /**
     * Sends [message] to POST /analyze and returns a typed [Result].
     *
     * **Must be called from a coroutine running on [kotlinx.coroutines.Dispatchers.IO].**
     * This function does not switch dispatchers itself — the caller owns that decision.
     *
     * @param message     The raw captured text to evaluate (notification body, screen text, …).
     * @param packageName Android package of the source app — used only for log context,
     *                    not included in the request body.
     * @return [Result.success] wrapping [ScamAnalysisResponse] on HTTP 2xx, or
     *         [Result.failure] wrapping the root exception on any error.
     */
    suspend fun analyzeText(
        message: String,
        packageName: String,
    ): Result<ScamAnalysisResponse> {

        // Log the outgoing request before suspending, so even a timeout shows what was sent.
        val preview = message.take(LOG_PREVIEW_LEN).replace('\n', ' ')
        val ellipsis = if (message.length > LOG_PREVIEW_LEN) "…" else ""
        logD(
            TAG,
            "→ POST /analyze | pkg=$packageName | ${message.length} chars | \"$preview$ellipsis\"",
        )

        return try {
            val response = api.analyzeText(ScamAnalysisRequest(message))

            // ── Success path ──────────────────────────────────────────────────
            logD(
                TAG,
                "✓ /analyze | pkg=$packageName | " +
                    "prob=${"%.3f".format(response.scamProbability)} | " +
                    "label=${response.label} | " +
                    "keywords(${response.suspiciousKeywords.size})=${response.suspiciousKeywords}",
            )

            Result.success(response)

        } catch (e: HttpException) {
            // ── HTTP error (4xx / 5xx) ────────────────────────────────────────
            // Common causes: 422 Unprocessable Entity (wrong request shape),
            //                500 Internal Server Error (model crashed), etc.
            Log.e(TAG, "✗ HTTP ${e.code()} | pkg=$packageName | ${e.message()}")
            Result.failure(e)

        } catch (e: IOException) {
            Log.e(TAG, "Backend unavailable: ${e.message}")
            Result.failure(BackendUnavailableException(e))

        } catch (e: Exception) {
            // ── Catch-all ─────────────────────────────────────────────────────
            // Gson JsonSyntaxException, JsonIOException, or unexpected runtime errors.
            Log.e(
                TAG,
                "✗ Unexpected | pkg=$packageName | " +
                    "${e.javaClass.simpleName}: ${e.message}",
                e,
            )
            Result.failure(e)
        }
    }
}
