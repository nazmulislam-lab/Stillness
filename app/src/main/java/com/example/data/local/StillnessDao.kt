package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StillnessDao {
    @Query("SELECT * FROM app_limits ORDER BY dailyLimitMinutes ASC")
    fun observeAppLimits(): Flow<List<AppLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppLimit(limit: AppLimitEntity)

    @Query("DELETE FROM app_limits WHERE packageName = :packageName")
    suspend fun deleteAppLimit(packageName: String)

    @Query("SELECT * FROM weekly_reports WHERE weekStartIso = :weekStartIso LIMIT 1")
    fun observeWeeklyReport(weekStartIso: String): Flow<WeeklyReportEntity?>

    @Query("SELECT * FROM weekly_reports ORDER BY generatedAtMillis DESC")
    fun observeAllWeeklyReports(): Flow<List<WeeklyReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyReport(report: WeeklyReportEntity)

    @Query("UPDATE weekly_reports SET completedSuggestionIdsCsv = :completedCsv WHERE weekStartIso = :weekStartIso")
    suspend fun updateCompletedSuggestions(weekStartIso: String, completedCsv: String)
}
