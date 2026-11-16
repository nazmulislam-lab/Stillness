package com.example.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.LruCache
import com.example.model.AppCategory
import com.example.model.AppUsageEntry
import com.example.model.DailyUsageSummary
import com.example.model.WeeklyUsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class DeviceUsageDataSource(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val packageManager = context.packageManager

    private val iconCache = LruCache<String, Drawable>(64)
    private val labelCache = mutableMapOf<String, String>()
    private val categoryCache = mutableMapOf<String, AppCategory>()

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun createUsageAccessIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        iconCache.get(packageName)?.let { return it }
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            iconCache.put(packageName, drawable)
            drawable
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    suspend fun loadWeeklySnapshot(
        windDownStartHour: Int,
        limitsByPackage: Map<String, Int>,
    ): WeeklyUsageSnapshot = withContext(Dispatchers.IO) {
        val nowMillis = System.currentTimeMillis()
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
        val rangeFormatter = SimpleDateFormat("MMM d", Locale.getDefault())

        val startCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }
        val weekStartMillis = startCal.timeInMillis
        val weekStartIso = isoFormatter.format(startCal.time)
        val endCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val weekRangeLabel = "${rangeFormatter.format(startCal.time)} – ${rangeFormatter.format(endCal.time)}"

        if (!hasUsageStatsPermission() || usageStatsManager == null) {
            val emptyDays = (0..6).map { offset ->
                val dayCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
                DailyUsageSummary(
                    dateIso = isoFormatter.format(dayCal.time),
                    dayLabel = if (offset == 6) "Today" else dayFormatter.format(dayCal.time),
                    totalTimeMillis = 0L,
                    totalLaunches = 0,
                    longestSessionMillis = 0L,
                    lateNightMillis = 0L,
                    topAppName = null,
                    topAppPackage = null,
                    categoryMinutes = emptyMap(),
                    hourlyMinutes = List(24) { 0 },
                    apps = emptyList(),
                )
            }
            return@withContext WeeklyUsageSnapshot(
                weekStartIso = weekStartIso,
                weekRangeLabel = weekRangeLabel,
                dailySummaries = emptyDays,
                averageDailyMillis = 0L,
                weekdayAverageMillis = 0L,
                weekendAverageMillis = 0L,
                totalLaunches = 0,
                lateNightTotalMillis = 0L,
                peakHour = 20,
                topCategory = AppCategory.OTHER,
                topApps = emptyList(),
                hasRealUsageData = false,
            )
        }

        val dailySummaries = mutableListOf<DailyUsageSummary>()
        val launcherPackage = resolveDefaultLauncherPackage()

        for (offset in 0..6) {
            val dayStartCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val dayStartMillis = dayStartCal.timeInMillis
            val dayEndMillis = if (offset == 6) {
                nowMillis
            } else {
                (dayStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis - 1L
            }

            val summary = computeDailySummary(
                dayStartMillis = dayStartMillis,
                dayEndMillis = dayEndMillis,
                dateIso = isoFormatter.format(dayStartCal.time),
                dayLabel = if (offset == 6) "Today" else dayFormatter.format(dayStartCal.time),
                windDownStartHour = windDownStartHour,
                limitsByPackage = limitsByPackage,
                excludedPackages = setOfNotNull(launcherPackage, "com.android.systemui"),
            )
            dailySummaries.add(summary)
        }

        val totalWeekMillis = dailySummaries.sumOf { it.totalTimeMillis }
        val activeDays = dailySummaries.count { it.totalTimeMillis > 0L }.coerceAtLeast(1)
        val averageDailyMillis = totalWeekMillis / activeDays

        val weekdayValues = mutableListOf<Long>()
        val weekendValues = mutableListOf<Long>()
        dailySummaries.forEachIndexed { idx, summary ->
            val cal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, idx) }
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                weekendValues.add(summary.totalTimeMillis)
            } else {
                weekdayValues.add(summary.totalTimeMillis)
            }
        }
        val weekdayAvg = if (weekdayValues.isNotEmpty()) weekdayValues.average().toLong() else 0L
        val weekendAvg = if (weekendValues.isNotEmpty()) weekendValues.average().toLong() else 0L

        val combinedHourly = IntArray(24)
        dailySummaries.forEach { day ->
            day.hourlyMinutes.forEachIndexed { h, mins -> combinedHourly[h] += mins }
        }
        val peakHour = combinedHourly.indices.maxByOrNull { combinedHourly[it] } ?: 20

        val categoryTotals = mutableMapOf<AppCategory, Long>()
        val appAccumulator = mutableMapOf<String, AppAccumulator>()
        dailySummaries.forEach { day ->
            day.categoryMinutes.forEach { (cat, millis) ->
                categoryTotals[cat] = (categoryTotals[cat] ?: 0L) + millis
            }
            day.apps.forEach { app ->
                val acc = appAccumulator.getOrPut(app.packageName) {
                    AppAccumulator(
                        packageName = app.packageName,
                        appName = app.appName,
                        category = app.category,
                    )
                }
                acc.totalTimeMillis += app.totalTimeMillis
                acc.launchCount += app.launchCount
                acc.lateNightMillis += app.lateNightMillis
                acc.lastUsedMillis = max(acc.lastUsedMillis, app.lastUsedMillis)
            }
        }

        val topCategory = categoryTotals.maxByOrNull { it.value }?.key ?: AppCategory.OTHER
        val topApps = appAccumulator.values
            .map { acc ->
                AppUsageEntry(
                    packageName = acc.packageName,
                    appName = acc.appName,
                    category = acc.category,
                    totalTimeMillis = acc.totalTimeMillis,
                    launchCount = acc.launchCount,
                    lastUsedMillis = acc.lastUsedMillis,
                    lateNightMillis = acc.lateNightMillis,
                    dailyLimitMinutes = limitsByPackage[acc.packageName],
                )
            }
            .sortedByDescending { it.totalTimeMillis }

        WeeklyUsageSnapshot(
            weekStartIso = weekStartIso,
            weekRangeLabel = weekRangeLabel,
            dailySummaries = dailySummaries,
            averageDailyMillis = averageDailyMillis,
            weekdayAverageMillis = weekdayAvg,
            weekendAverageMillis = weekendAvg,
            totalLaunches = dailySummaries.sumOf { it.totalLaunches },
            lateNightTotalMillis = dailySummaries.sumOf { it.lateNightMillis },
            peakHour = peakHour,
            topCategory = topCategory,
            topApps = topApps,
            hasRealUsageData = totalWeekMillis > 0L || topApps.isNotEmpty(),
        )
    }

    private fun computeDailySummary(
        dayStartMillis: Long,
        dayEndMillis: Long,
        dateIso: String,
        dayLabel: String,
        windDownStartHour: Int,
        limitsByPackage: Map<String, Int>,
        excludedPackages: Set<String>,
    ): DailyUsageSummary {
        val manager = usageStatsManager ?: return emptyDailySummary(dateIso, dayLabel)

        val activeStarts = mutableMapOf<String, Long>()
        val durationsByPackage = mutableMapOf<String, Long>()
        val launchesByPackage = mutableMapOf<String, Int>()
        val lastUsedByPackage = mutableMapOf<String, Long>()
        val lateNightByPackage = mutableMapOf<String, Long>()
        val hourlyMillis = LongArray(24)
        var longestSessionMillis = 0L

        val events = manager.queryEvents(dayStartMillis, dayEndMillis)
        val event = UsageEvents.Event()
        val lastResumeByPackage = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg in excludedPackages) continue

            val timestamp = event.timeStamp
            val type = event.eventType

            val isForeground = type == UsageEvents.Event.ACTIVITY_RESUMED ||
                @Suppress("DEPRECATION") (type == UsageEvents.Event.MOVE_TO_FOREGROUND)
            val isBackground = type == UsageEvents.Event.ACTIVITY_PAUSED ||
                type == UsageEvents.Event.ACTIVITY_STOPPED ||
                @Suppress("DEPRECATION") (type == UsageEvents.Event.MOVE_TO_BACKGROUND)

            if (isForeground) {
                if (!activeStarts.containsKey(pkg)) {
                    activeStarts[pkg] = timestamp
                }
                val prevResume = lastResumeByPackage[pkg] ?: 0L
                if (timestamp - prevResume > 12_000L) {
                    launchesByPackage[pkg] = (launchesByPackage[pkg] ?: 0) + 1
                }
                lastResumeByPackage[pkg] = timestamp
                lastUsedByPackage[pkg] = max(lastUsedByPackage[pkg] ?: 0L, timestamp)
            } else if (isBackground) {
                val start = activeStarts.remove(pkg)
                if (start != null && timestamp > start) {
                    val sessionDuration = (timestamp - start).coerceAtMost(8 * 3_600_000L)
                    durationsByPackage[pkg] = (durationsByPackage[pkg] ?: 0L) + sessionDuration
                    longestSessionMillis = max(longestSessionMillis, sessionDuration)
                    lastUsedByPackage[pkg] = max(lastUsedByPackage[pkg] ?: 0L, timestamp)
                    val lateMillis = distributeSessionAcrossHours(
                        startMillis = start,
                        endMillis = start + sessionDuration,
                        hourlyMillis = hourlyMillis,
                        windDownStartHour = windDownStartHour,
                    )
                    lateNightByPackage[pkg] = (lateNightByPackage[pkg] ?: 0L) + lateMillis
                }
            }
        }

        // Close any currently open foreground session up to dayEndMillis
        activeStarts.forEach { (pkg, start) ->
            if (dayEndMillis > start) {
                val sessionDuration = (dayEndMillis - start).coerceAtMost(4 * 3_600_000L)
                durationsByPackage[pkg] = (durationsByPackage[pkg] ?: 0L) + sessionDuration
                longestSessionMillis = max(longestSessionMillis, sessionDuration)
                lastUsedByPackage[pkg] = max(lastUsedByPackage[pkg] ?: 0L, dayEndMillis)
                val lateMillis = distributeSessionAcrossHours(
                    startMillis = start,
                    endMillis = start + sessionDuration,
                    hourlyMillis = hourlyMillis,
                    windDownStartHour = windDownStartHour,
                )
                lateNightByPackage[pkg] = (lateNightByPackage[pkg] ?: 0L) + lateMillis
            }
        }

        // Fallback/augmentation from aggregated UsageStats when OS trimmed older raw events
        val aggregateStats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            dayStartMillis,
            dayEndMillis,
        )
        aggregateStats?.forEach { stat ->
            val pkg = stat.packageName ?: return@forEach
            if (pkg in excludedPackages) return@forEach
            if (stat.lastTimeUsed < dayStartMillis) return@forEach
            val aggDuration = stat.totalTimeInForeground
            val existing = durationsByPackage[pkg] ?: 0L
            if (existing == 0L && aggDuration > 30_000L) {
                durationsByPackage[pkg] = aggDuration
                lastUsedByPackage[pkg] = max(lastUsedByPackage[pkg] ?: 0L, stat.lastTimeUsed)
                if ((launchesByPackage[pkg] ?: 0) == 0) {
                    launchesByPackage[pkg] = 1
                }
            }
        }

        val appEntries = durationsByPackage.entries
            .filter { it.value >= 15_000L } // filter sub-15s transient system calls
            .map { (pkg, totalMillis) ->
                val appName = resolveAppName(pkg)
                val category = resolveAppCategory(pkg)
                AppUsageEntry(
                    packageName = pkg,
                    appName = appName,
                    category = category,
                    totalTimeMillis = totalMillis,
                    launchCount = max(launchesByPackage[pkg] ?: 1, 1),
                    lastUsedMillis = lastUsedByPackage[pkg] ?: dayStartMillis,
                    lateNightMillis = lateNightByPackage[pkg] ?: 0L,
                    dailyLimitMinutes = limitsByPackage[pkg],
                )
            }
            .sortedByDescending { it.totalTimeMillis }

        val categoryMinutes = mutableMapOf<AppCategory, Long>()
        appEntries.forEach { entry ->
            categoryMinutes[entry.category] = (categoryMinutes[entry.category] ?: 0L) + entry.totalTimeMillis
        }

        val totalTimeMillis = appEntries.sumOf { it.totalTimeMillis }
        val totalLaunches = appEntries.sumOf { it.launchCount }
        val lateNightMillis = appEntries.sumOf { it.lateNightMillis }
        val topApp = appEntries.firstOrNull()

        return DailyUsageSummary(
            dateIso = dateIso,
            dayLabel = dayLabel,
            totalTimeMillis = totalTimeMillis,
            totalLaunches = totalLaunches,
            longestSessionMillis = longestSessionMillis,
            lateNightMillis = lateNightMillis,
            topAppName = topApp?.appName,
            topAppPackage = topApp?.packageName,
            categoryMinutes = categoryMinutes,
            hourlyMinutes = hourlyMillis.map { (it / 60_000L).toInt() },
            apps = appEntries,
        )
    }

    private fun distributeSessionAcrossHours(
        startMillis: Long,
        endMillis: Long,
        hourlyMillis: LongArray,
        windDownStartHour: Int,
    ): Long {
        var cursor = startMillis
        var lateNightTotal = 0L
        val cal = Calendar.getInstance()

        while (cursor < endMillis) {
            cal.timeInMillis = cursor
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val hourEnd = min(endMillis, cal.timeInMillis + 1L)
            val slice = max(0L, hourEnd - cursor)
            if (hour in 0..23) {
                hourlyMillis[hour] += slice
                if (hour >= windDownStartHour || hour < 5) {
                    lateNightTotal += slice
                }
            }
            cursor = hourEnd
        }
        return lateNightTotal
    }

    private fun resolveDefaultLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    private fun resolveAppName(packageName: String): String {
        labelCache[packageName]?.let { return it }
        val name = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.').replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
        labelCache[packageName] = name
        return name
    }

    private fun resolveAppCategory(packageName: String): AppCategory {
        categoryCache[packageName]?.let { return it }
        val lower = packageName.lowercase(Locale.US)
        val heuristic = when {
            lower.contains("instagram") || lower.contains("tiktok") ||
                lower.contains("musically") || lower.contains("twitter") ||
                lower.contains("reddit") || lower.contains("facebook") ||
                lower.contains("snapchat") || lower.contains("threads") -> AppCategory.SOCIAL
            lower.contains("youtube") || lower.contains("netflix") ||
                lower.contains("spotify") || lower.contains("twitch") ||
                lower.contains("disney") || lower.contains("primevideo") -> AppCategory.VIDEO
            lower.contains("whatsapp") || lower.contains("messaging") ||
                lower.contains("telegram") || lower.contains("discord") ||
                lower.contains("slack") || lower.contains("signal") -> AppCategory.COMMUNICATION
            lower.contains("docs") || lower.contains("sheets") ||
                lower.contains("notion") || lower.contains("calendar") ||
                lower.contains("mail") || lower.contains("office") ||
                lower.contains("chrome") || lower.contains("notes") -> AppCategory.PRODUCTIVITY
            else -> null
        }

        val resolved = heuristic ?: try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (appInfo.category) {
                    ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
                    ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_AUDIO -> AppCategory.VIDEO
                    ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
                    ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_NEWS -> AppCategory.PRODUCTIVITY
                    else -> AppCategory.OTHER
                }
            } else {
                AppCategory.OTHER
            }
        } catch (_: PackageManager.NameNotFoundException) {
            AppCategory.OTHER
        }

        categoryCache[packageName] = resolved
        return resolved
    }

    private fun emptyDailySummary(dateIso: String, dayLabel: String) = DailyUsageSummary(
        dateIso = dateIso,
        dayLabel = dayLabel,
        totalTimeMillis = 0L,
        totalLaunches = 0,
        longestSessionMillis = 0L,
        lateNightMillis = 0L,
        topAppName = null,
        topAppPackage = null,
        categoryMinutes = emptyMap(),
        hourlyMinutes = List(24) { 0 },
        apps = emptyList(),
    )

    private data class AppAccumulator(
        val packageName: String,
        val appName: String,
        val category: AppCategory,
        var totalTimeMillis: Long = 0L,
        var launchCount: Int = 0,
        var lateNightMillis: Long = 0L,
        var lastUsedMillis: Long = 0L,
    )
}
