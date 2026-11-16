package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppLimitEntity::class, WeeklyReportEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class StillnessDatabase : RoomDatabase() {
    abstract fun stillnessDao(): StillnessDao

    companion object {
        @Volatile
        private var INSTANCE: StillnessDatabase? = null

        fun getInstance(context: Context): StillnessDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StillnessDatabase::class.java,
                    "stillness_db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
