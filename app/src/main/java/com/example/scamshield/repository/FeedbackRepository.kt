package com.example.scamshield.repository

import android.content.Context
import com.example.scamshield.data.db.FeedbackDao
import com.example.scamshield.data.db.FeedbackEntity
import com.example.scamshield.data.db.ScamShieldDatabase
import kotlinx.coroutines.flow.Flow

class FeedbackRepository(private val dao: FeedbackDao) {

    suspend fun insert(feedback: FeedbackEntity): Long = dao.insert(feedback)

    fun observeAll(): Flow<List<FeedbackEntity>> = dao.observeAll()

    companion object {
        @Volatile
        private var INSTANCE: FeedbackRepository? = null

        fun get(context: Context): FeedbackRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FeedbackRepository(
                    ScamShieldDatabase.get(context).feedbackDao()
                ).also { INSTANCE = it }
            }
    }
}
