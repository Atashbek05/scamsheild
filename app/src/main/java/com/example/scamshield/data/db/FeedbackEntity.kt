package com.example.scamshield.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "threat_id")
    val threatId: Long,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "comment")
    val comment: String = "",

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
)
