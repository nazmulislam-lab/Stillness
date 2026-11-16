package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_limits")
data class AppLimitEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val updatedAtMillis: Long,
)

@Entity(tableName = "weekly_reports")
data class WeeklyReportEntity(
    @PrimaryKey val weekStartIso: String,
    val generatedAtMillis: Long,
    val isSamplePlaceholder: Boolean,
    val headline: String,
    val summary: String,
    val wellBeingScore: Int,
    val patternHighlightsJson: String,
    val suggestionsJson: String,
    val completedSuggestionIdsCsv: String,
)
