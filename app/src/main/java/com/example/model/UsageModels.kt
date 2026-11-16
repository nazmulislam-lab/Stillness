package com.example.model

enum class AppCategory(val label: String) {
    SOCIAL("Social"),
    VIDEO("Video & Audio"),
    GAMES("Games"),
    COMMUNICATION("Messaging"),
    PRODUCTIVITY("Productivity"),
    OTHER("Utilities & Other"),
}

data class AppUsageEntry(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val totalTimeMillis: Long,
    val launchCount: Int,
    val lastUsedMillis: Long,
    val lateNightMillis: Long,
    val dailyLimitMinutes: Int? = null,
) {
    val totalMinutes: Int
        get() = (totalTimeMillis / 60_000L).toInt()
}

data class DailyUsageSummary(
    val dateIso: String,
    val dayLabel: String,
    val totalTimeMillis: Long,
    val totalLaunches: Int,
    val longestSessionMillis: Long,
    val lateNightMillis: Long,
    val topAppName: String?,
    val topAppPackage: String?,
    val categoryMinutes: Map<AppCategory, Long>,
    val hourlyMinutes: List<Int>,
    val apps: List<AppUsageEntry>,
)

data class WeeklyUsageSnapshot(
    val weekStartIso: String,
    val weekRangeLabel: String,
    val dailySummaries: List<DailyUsageSummary>,
    val averageDailyMillis: Long,
    val weekdayAverageMillis: Long,
    val weekendAverageMillis: Long,
    val totalLaunches: Int,
    val lateNightTotalMillis: Long,
    val peakHour: Int,
    val topCategory: AppCategory,
    val topApps: List<AppUsageEntry>,
    val hasRealUsageData: Boolean,
)

data class WellBeingSuggestion(
    val id: String,
    val title: String,
    val rationale: String,
    val actionStep: String,
    val focusTag: String,
    val impactMinutesPerWeek: Int,
    val targetAppName: String? = null,
    val isCompleted: Boolean = false,
)

data class WeeklyAdvisorReport(
    val weekStartIso: String,
    val generatedAtMillis: Long,
    val isSamplePlaceholder: Boolean,
    val headline: String,
    val summary: String,
    val wellBeingScore: Int,
    val patternHighlights: List<String>,
    val suggestions: List<WellBeingSuggestion>,
)

data class UserWellBeingSettings(
    val dailyGoalMinutes: Int = 180,
    val windDownStartHour: Int = 22,
    val weeklyReviewDayOfWeek: Int = 7, // 1 = Mon .. 7 = Sun
)
