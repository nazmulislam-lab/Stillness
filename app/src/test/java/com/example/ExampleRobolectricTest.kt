package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.advisor.PlaceholderWellBeingAdvisorRepository
import com.example.model.AppCategory
import com.example.model.AppUsageEntry
import com.example.model.DailyUsageSummary
import com.example.model.WeeklyUsageSnapshot
import com.example.ui.util.formatDurationMinutes
import com.example.ui.util.formatHour24
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Stillness`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Stillness", appName)
    }

    @Test
    fun `formatters format durations and hours cleanly`() {
        assertEquals("2h 15m", formatDurationMinutes(135))
        assertEquals("45m", formatDurationMinutes(45))
        assertEquals("10 PM", formatHour24(22))
        assertEquals("12 AM", formatHour24(0))
    }

    @Test
    fun `placeholder advisor generates labeled weekly suggestions`() = runTest {
        val advisor = PlaceholderWellBeingAdvisorRepository()
        val snapshot = WeeklyUsageSnapshot(
            weekStartIso = "2026-11-09",
            weekRangeLabel = "Nov 9 – Nov 15",
            dailySummaries = List(7) { idx ->
                DailyUsageSummary(
                    dateIso = "2026-11-0${idx + 1}",
                    dayLabel = "Day $idx",
                    totalTimeMillis = 210 * 60_000L,
                    totalLaunches = 42,
                    longestSessionMillis = 48 * 60_000L,
                    lateNightMillis = 35 * 60_000L,
                    topAppName = "VideoStream",
                    topAppPackage = "com.sample.video",
                    categoryMinutes = mapOf(AppCategory.VIDEO to 120 * 60_000L),
                    hourlyMinutes = List(24) { if (it == 22) 35 else 5 },
                    apps = emptyList(),
                )
            },
            averageDailyMillis = 210 * 60_000L,
            weekdayAverageMillis = 200 * 60_000L,
            weekendAverageMillis = 235 * 60_000L,
            totalLaunches = 294,
            lateNightTotalMillis = 245 * 60_000L,
            peakHour = 22,
            topCategory = AppCategory.VIDEO,
            topApps = listOf(
                AppUsageEntry(
                    packageName = "com.sample.video",
                    appName = "VideoStream",
                    category = AppCategory.VIDEO,
                    totalTimeMillis = 840 * 60_000L,
                    launchCount = 70,
                    lastUsedMillis = System.currentTimeMillis(),
                    lateNightMillis = 180 * 60_000L,
                ),
            ),
            hasRealUsageData = true,
        )

        val report = advisor.generateWeeklyReport(
            snapshot = snapshot,
            dailyGoalMinutes = 180,
            windDownStartHour = 22,
        )

        assertTrue(report.isSamplePlaceholder)
        assertTrue(report.suggestions.isNotEmpty())
        assertTrue(report.suggestions.first().title.contains("VideoStream"))
    }
}
