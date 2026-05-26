package com.example.scamshield.service

object ScanPauseManager {

    private const val PAUSE_DURATION_MS = 60 * 60 * 1_000L // 1 hour

    @Volatile private var pauseUntil: Long = 0L

    fun pauseForOneHour() {
        pauseUntil = System.currentTimeMillis() + PAUSE_DURATION_MS
    }

    fun resume() {
        pauseUntil = 0L
    }

    fun isPaused(): Boolean = System.currentTimeMillis() < pauseUntil
}
