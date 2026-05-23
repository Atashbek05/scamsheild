package com.example.scamshield.data

enum class MonitorSource { NOTIFICATION_LISTENER, ACCESSIBILITY_SERVICE }

enum class ScanStatus { SCANNING, ANALYZING, THREAT_DETECTED, SAFE_MESSAGE, ERROR }

data class MonitoringEvent(
    val id: Long = System.nanoTime(),
    val packageName: String,
    val appLabel: String,
    val textPreview: String,
    val status: ScanStatus,
    val probability: Float? = null,
    val source: MonitorSource,
    val timestamp: Long = System.currentTimeMillis(),
)
