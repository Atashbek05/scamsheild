package com.example.scamshield.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted record of an incoming call analysed by the call-protection pipeline.
 * Powers the call-history list, top-scam-callers tile, and daily threat stats
 * on the Call Protection dashboard.
 */
@Entity(
    tableName = "calls",
    indices = [Index("phone_number"), Index("timestamp")],
)
data class CallEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "probability")
    val probability: Float,

    @ColumnInfo(name = "risk")
    val risk: String, // CallRisk.name

    @ColumnInfo(name = "flags")
    val flags: String, // CallFlag names joined with "|"

    @ColumnInfo(name = "explanation")
    val explanation: String,

    @ColumnInfo(name = "action")
    val action: String, // ALLOWED / SCREENED / BLOCKED / REPORTED

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
)

/**
 * A phone number explicitly blocked by the user.  CallScreeningService rejects
 * calls when the incoming number matches an entry here.
 */
@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "blocked_at")
    val blockedAt: Long,
)

/**
 * A number marked by the user as safe — calls bypass the risk analyser.
 */
@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long,
)
