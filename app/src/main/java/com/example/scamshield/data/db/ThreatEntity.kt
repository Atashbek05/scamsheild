package com.example.scamshield.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted representation of a detected scam threat.
 *
 * Stored in Room so the threat history, analytics, and PDF report can survive
 * process death and device reboots. The in-memory [com.example.scamshield.data.ThreatStore]
 * remains the hot cache for the dashboard's live feed; this is the durable layer.
 */
@Entity(tableName = "threats")
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "source_package")
    val sourcePackage: String,

    @ColumnInfo(name = "app_label")
    val appLabel: String,

    @ColumnInfo(name = "message_preview")
    val messagePreview: String,

    @ColumnInfo(name = "probability")
    val probability: Float,

    @ColumnInfo(name = "risk_level")
    val riskLevel: String, // HIGH / MEDIUM / LOW / SAFE

    @ColumnInfo(name = "category")
    val category: String, // PHISHING / OTP / FAKE_BANKING / SOCIAL_ENGINEERING / OTHER

    @ColumnInfo(name = "backend_label")
    val backendLabel: String,

    @ColumnInfo(name = "keywords")
    val keywords: String, // joined with " ▸ "

    @ColumnInfo(name = "phishing_links")
    val phishingLinks: String,

    @ColumnInfo(name = "urgency_patterns")
    val urgencyPatterns: String,

    @ColumnInfo(name = "social_indicators")
    val socialIndicators: String,

    @ColumnInfo(name = "explanation")
    val explanation: String,

    @ColumnInfo(name = "analysis_source")
    val analysisSource: String, // BACKEND / LOCAL / COMBINED

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
)
