package com.example.scamshield.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feedback: FeedbackEntity): Long

    @Query("SELECT * FROM feedback ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<FeedbackEntity>>
}
