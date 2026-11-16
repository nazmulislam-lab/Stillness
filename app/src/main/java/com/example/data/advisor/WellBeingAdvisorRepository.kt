package com.example.data.advisor

import com.example.model.WeeklyAdvisorReport
import com.example.model.WeeklyUsageSnapshot
import com.example.model.WellBeingSuggestion
import kotlinx.coroutines.delay

/**
 * Contract for generating weekly digital well-being suggestions from mobile usage patterns.
 * Backed by a clearly labeled placeholder implementation when Firebase AI Logic is not enabled.
 */
interface WellBeingAdvisorRepository {
    suspend fun generateWeeklyReport(
        snapshot: WeeklyUsageSnapshot,
        dailyGoalMinutes: Int,
        windDownStartHour: Int,
    ): WeeklyAdvisorReport
}

class PlaceholderWellBeingAdvisorRepository : WellBeingAdvisorRepository {

    override suspend fun generateWeeklyReport(
        snapshot: WeeklyUsageSnapshot,
        dailyGoalMinutes: Int,
        windDownStartHour: Int,
    ): WeeklyAdvisorReport {
        delay(350) // brief responsive transition

        if (!snapshot.hasRealUsageData) {
            return WeeklyAdvisorReport(
                weekStartIso = snapshot.weekStartIso,
                generatedAtMillis = System.currentTimeMillis(),
                isSamplePlaceholder = true,
                headline = "Sample Weekly Rhythm Preview",
                summary = "Grant Usage Access to analyze your phone's actual 7-day patterns. Below is a sample weekly coaching plan illustrating how Stillness structures mindful habit shifts.",
                wellBeingScore = 74,
                patternHighlights = listOf(
                    "Sample pattern: Evening screen time peaks between 9 PM and 11 PM",
                    "Sample pattern: Short frequent pickups add ~45 min of fragmented attention daily",
                    "Sample pattern: Weekend screen time averages 24% higher than weekdays",
                ),
                suggestions = listOf(
                    WellBeingSuggestion(
                        id = "sample_wind_down",
                        title = "Anchor a ${formatHour(windDownStartHour)} Wind-Down Buffer",
                        rationale = "Late-evening stimulation delays sleep onset and increases morning scroll impulses.",
                        actionStep = "Park your phone on a charger outside arm's reach 30 minutes before ${formatHour(windDownStartHour)}.",
                        focusTag = "Wind-Down",
                        impactMinutesPerWeek = 140,
                        targetAppName = "Evening Routine",
                    ),
                    WellBeingSuggestion(
                        id = "sample_batch_checks",
                        title = "Batch Midday Check-Ins",
                        rationale = "Frequent sub-2-minute pickups fragment deep focus without delivering meaningful utility.",
                        actionStep = "Keep messaging and social notifications muted during morning focus blocks and check once at noon.",
                        focusTag = "Pickups",
                        impactMinutesPerWeek = 95,
                        targetAppName = "Messaging & Social",
                    ),
                    WellBeingSuggestion(
                        id = "sample_weekend_anchor",
                        title = "Create a Screen-Free Morning Window",
                        rationale = "First-hour screen usage sets a reactive tone for the entire weekend.",
                        actionStep = "Wait 20 minutes after waking before opening your first social or video app.",
                        focusTag = "Morning Calm",
                        impactMinutesPerWeek = 110,
                    ),
                ),
            )
        }

        val avgMinutes = (snapshot.averageDailyMillis / 60_000L).toInt()
        val lateNightMinutes = (snapshot.lateNightTotalMillis / 60_000L).toInt()
        val topApp = snapshot.topApps.firstOrNull()
        val secondApp = snapshot.topApps.getOrNull(1)
        val avgLaunches = snapshot.totalLaunches / 7
        val weekendDelta = if (snapshot.weekdayAverageMillis > 0L) {
            (((snapshot.weekendAverageMillis - snapshot.weekdayAverageMillis).toDouble() /
                snapshot.weekdayAverageMillis.toDouble()) * 100.0).toInt()
        } else {
            0
        }

        val goalRatio = avgMinutes.toFloat() / dailyGoalMinutes.coerceAtLeast(30).toFloat()
        val lateRatio = if (snapshot.averageDailyMillis > 0L) {
            (snapshot.lateNightTotalMillis.toFloat() / 7f) / snapshot.averageDailyMillis.toFloat()
        } else {
            0f
        }
        val score = (100 - ((goalRatio - 0.85f).coerceAtLeast(0f) * 38f) - (lateRatio * 45f))
            .toInt()
            .coerceIn(32, 96)

        val highlights = buildList {
            add(
                "Daily average is ${formatMinutes(avgMinutes)} against your ${formatMinutes(dailyGoalMinutes)} budget (${snapshot.topCategory.label} leads).",
            )
            if (lateNightMinutes > 15) {
                add(
                    "${formatMinutes(lateNightMinutes)} spent after ${formatHour(windDownStartHour)} over the past 7 days.",
                )
            } else {
                add(
                    "Low late-night activity: only ${formatMinutes(lateNightMinutes)} after ${formatHour(windDownStartHour)} this week.",
                )
            }
            if (weekendDelta > 10) {
                add("Weekend usage runs $weekendDelta% higher than weekdays; peak activity hits around ${formatHour(snapshot.peakHour)}.")
            } else if (weekendDelta < -10) {
                add("Weekdays carry ${-weekendDelta}% more screen time than weekends, peaking around ${formatHour(snapshot.peakHour)}.")
            } else {
                add("Consistent daily rhythm across the week, with peak screen time around ${formatHour(snapshot.peakHour)}.")
            }
        }

        val suggestions = buildList {
            if (topApp != null) {
                val topWeeklyMin = topApp.totalMinutes
                val suggestedCap = ((topWeeklyMin / 7) * 0.8f).toInt().coerceIn(15, 120)
                add(
                    WellBeingSuggestion(
                        id = "sample_top_app_${topApp.packageName}",
                        title = "Set a ${formatMinutes(suggestedCap)} Daily Budget for ${topApp.appName}",
                        rationale = "${topApp.appName} accounted for ${formatMinutes(topWeeklyMin)} across ${topApp.launchCount} opens this week.",
                        actionStep = "Tap ${topApp.appName} in the Apps tab and set a ${formatMinutes(suggestedCap)}/day reminder to trim ~20% of passive time.",
                        focusTag = "App Budget",
                        impactMinutesPerWeek = (topWeeklyMin * 0.2f).toInt().coerceAtLeast(25),
                        targetAppName = topApp.appName,
                    ),
                )
            }
            add(
                WellBeingSuggestion(
                    id = "sample_wind_down",
                    title = if (lateNightMinutes > 30) {
                        "Protect Sleep After ${formatHour(windDownStartHour)}"
                    } else {
                        "Keep Your ${formatHour(windDownStartHour)} Wind-Down Streak"
                    },
                    rationale = if (lateNightMinutes > 30) {
                        "You logged ${formatMinutes(lateNightMinutes)} during wind-down hours this week, shifting your rest window later."
                    } else {
                        "Your late-night usage stayed under ${formatMinutes(lateNightMinutes)} this week — preserving recovery quality."
                    },
                    actionStep = "Schedule Grayscale or Bedtime Mode at ${formatHour(windDownStartHour)} and leave your device off the nightstand.",
                    focusTag = "Wind-Down",
                    impactMinutesPerWeek = (lateNightMinutes * 0.5f).toInt().coerceAtLeast(30),
                ),
            )
            add(
                WellBeingSuggestion(
                    id = "sample_pickups",
                    title = if (avgLaunches > 45) {
                        "Reduce Fragmented Pickups ($avgLaunches/day)"
                    } else {
                        "Guard Peak Focus Around ${formatHour(snapshot.peakHour)}"
                    },
                    rationale = if (secondApp != null) {
                        "Frequent switches into ${secondApp.appName} and ${snapshot.topCategory.label} apps break sustained attention."
                    } else {
                        "Activity clusters strongly around ${formatHour(snapshot.peakHour)}, making that hour ideal for intentional boundaries."
                    },
                    actionStep = "Batch non-urgent app checks into two 15-minute windows instead of opening on impulse.",
                    focusTag = "Pickups",
                    impactMinutesPerWeek = 70,
                    targetAppName = secondApp?.appName,
                ),
            )
        }

        val headline = when {
            score >= 82 -> "Steady & Intentional Week"
            score >= 65 -> "Balanced Rhythm With Evening Drift"
            else -> "High Screen Load — Gentle Reset Suggested"
        }

        return WeeklyAdvisorReport(
            weekStartIso = snapshot.weekStartIso,
            generatedAtMillis = System.currentTimeMillis(),
            isSamplePlaceholder = true,
            headline = headline,
            summary = "Sample coaching built from your ${snapshot.weekRangeLabel} on-device stats (${formatMinutes(avgMinutes)}/day average). Connect Firebase AI Logic for live Gemini pattern coaching.",
            wellBeingScore = score,
            patternHighlights = highlights,
            suggestions = suggestions,
        )
    }

    private fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    private fun formatHour(hour24: Int): String {
        val normalized = ((hour24 % 24) + 24) % 24
        val suffix = if (normalized < 12) "AM" else "PM"
        val hour12 = when {
            normalized == 0 -> 12
            normalized > 12 -> normalized - 12
            else -> normalized
        }
        return "$hour12 $suffix"
    }
}
