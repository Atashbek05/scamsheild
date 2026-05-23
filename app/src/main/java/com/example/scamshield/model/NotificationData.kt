package com.example.scamshield.model

/**
 * Holds the extracted content from a single incoming notification.
 * Only populated after filtering out empty/irrelevant notifications.
 */
data class NotificationData(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
