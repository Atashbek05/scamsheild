package com.example.scamshield.network

import com.example.scamshield.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton that owns the Retrofit + OkHttp stack for the ScamShield backend.
 *
 * Target: https://scamsheil-backend.onrender.com  (Render.com hosted FastAPI)
 *
 * Timeout rationale:
 *   Render.com free-tier instances spin down after 15 min of inactivity and can
 *   take 30–60 s to cold-start. The generous read timeout prevents false network
 *   errors during those first-request wake-ups.
 */
object ApiClient {

    private const val BASE_URL = "https://scamsheil-backend.onrender.com/"

    // Connect timeout: time to establish the TCP connection.
    private const val CONNECT_TIMEOUT_S = 30L

    // Read timeout: time to receive the full response body after the request is sent.
    // Set high to survive Render cold-starts without a spurious IOException.
    private const val READ_TIMEOUT_S = 60L

    // Write timeout: time to send the full request body to the server.
    private const val WRITE_TIMEOUT_S = 30L

    /**
     * OkHttp logging interceptor.
     *
     * BODY level logs the full request/response (headers + body) — useful in
     * development but noisy and a privacy risk in release builds. Switches to
     * NONE automatically for non-debug builds via BuildConfig.DEBUG.
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_S,    TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_S,  TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ScamShieldApiService = retrofit.create(ScamShieldApiService::class.java)
}
