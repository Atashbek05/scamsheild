package com.example.scamshield.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ThreatEntity::class,
        CallEntity::class,
        BlockedNumberEntity::class,
        TrustedContactEntity::class,
        FeedbackEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ScamShieldDatabase : RoomDatabase() {

    abstract fun threatDao(): ThreatDao
    abstract fun callDao(): CallDao
    abstract fun feedbackDao(): FeedbackDao

    companion object {
        private const val DB_NAME = "scamshield.db"

        @Volatile
        private var INSTANCE: ScamShieldDatabase? = null

        fun get(context: Context): ScamShieldDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScamShieldDatabase::class.java,
                    DB_NAME,
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
