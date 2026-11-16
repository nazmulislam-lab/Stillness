package com.example.data

import android.content.Intent
import android.graphics.drawable.Drawable
import com.example.data.advisor.WellBeingAdvisorRepository
import com.example.data.local.AppLimitEntity
import com.example.data.local.StillnessDao
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.WeeklyReportEntity
import com.example.data.usage.DeviceUsageDataSource
import com.example.model.UserWellBeingSettings
import com.example.model.WeeklyAdvisorReport
import com.example.model.WeeklyUsageSnapshot
import com.example.model.WellBeingSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class UsageRepository(
    private val usageDataSource: DeviceUsageDataSource,
    private val dao: StillnessDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val advisorRepository: WellBeingAdvisorRepository,
) {
    val settingsFlow: Flow<UserWellBeingSettings> = preferencesRepository.settings

    val appLimitsFlow: Flow<Map<String, Int>> = dao.observeAppLimits().map { list ->
        list.associate { it.packageName to it.dailyLimitMinutes }
    }

    val allAppLimitEntitiesFlow: Flow<List<AppLimitEntity>> = dao.observeAppLimits()

    fun hasUsageAccess(): Boolean = usageDataSource.hasUsageStatsPermission()

    fun createUsageAccessIntent(): Intent = usageDataSource.createUsageAccessIntent()

    fun getAppIcon(packageName: String): Drawable? = usageDataSource.getAppIcon(packageName)

    suspend fun loadWeeklySnapshot(
        windDownStartHour: Int,
        limitsByPackage: Map<String, Int>,
    ): WeeklyUsageSnapshot = usageDataSource.loadWeeklySnapshot(windDownStartHour, limitsByPackage)

    fun observeWeeklyReport(weekStartIso: String): Flow<WeeklyAdvisorReport?> =
        dao.observeWeeklyReport(weekStartIso).map { entity -> entity?.toDomain() }

    suspend fun generateAndSaveWeeklyReport(
        snapshot: WeeklyUsageSnapshot,
        dailyGoalMinutes: Int,
        windDownStartHour: Int,
    ): WeeklyAdvisorReport {
        val generated = advisorRepository.generateWeeklyReport(
            snapshot = snapshot,
            dailyGoalMinutes = dailyGoalMinutes,
            windDownStartHour = windDownStartHour,
        )
        dao.upsertWeeklyReport(generated.toEntity())
        return generated
    }

    suspend fun toggleSuggestionCompleted(
        report: WeeklyAdvisorReport,
        suggestionId: String,
    ) {
        val completedIds = report.suggestions
            .filter { if (it.id == suggestionId) !it.isCompleted else it.isCompleted }
            .map { it.id }
            .toSet()
        dao.updateCompletedSuggestions(
            weekStartIso = report.weekStartIso,
            completedCsv = completedIds.joinToString(","),
        )
    }

    suspend fun saveAppLimit(packageName: String, appName: String, limitMinutes: Int) {
        dao.upsertAppLimit(
            AppLimitEntity(
                packageName = packageName,
                appName = appName,
                dailyLimitMinutes = limitMinutes.coerceIn(5, 480),
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun removeAppLimit(packageName: String) {
        dao.deleteAppLimit(packageName)
    }

    suspend fun updateDailyGoalMinutes(minutes: Int) =
        preferencesRepository.updateDailyGoalMinutes(minutes)

    suspend fun updateWindDownStartHour(hour: Int) =
        preferencesRepository.updateWindDownStartHour(hour)

    suspend fun updateWeeklyReviewDay(dayOfWeek: Int) =
        preferencesRepository.updateWeeklyReviewDay(dayOfWeek)

    private fun WeeklyAdvisorReport.toEntity(): WeeklyReportEntity {
        val highlightsArray = JSONArray().apply {
            patternHighlights.forEach { put(it) }
        }
        val suggestionsArray = JSONArray().apply {
            suggestions.forEach { item ->
                put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("rationale", item.rationale)
                        put("actionStep", item.actionStep)
                        put("focusTag", item.focusTag)
                        put("impactMinutesPerWeek", item.impactMinutesPerWeek)
                        if (item.targetAppName != null) put("targetAppName", item.targetAppName)
                    },
                )
            }
        }
        val completedCsv = suggestions.filter { it.isCompleted }.joinToString(",") { it.id }
        return WeeklyReportEntity(
            weekStartIso = weekStartIso,
            generatedAtMillis = generatedAtMillis,
            isSamplePlaceholder = isSamplePlaceholder,
            headline = headline,
            summary = summary,
            wellBeingScore = wellBeingScore,
            patternHighlightsJson = highlightsArray.toString(),
            suggestionsJson = suggestionsArray.toString(),
            completedSuggestionIdsCsv = completedCsv,
        )
    }

    private fun WeeklyReportEntity.toDomain(): WeeklyAdvisorReport {
        val completedSet = completedSuggestionIdsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        val highlights = mutableListOf<String>()
        val hArr = JSONArray(patternHighlightsJson)
        for (i in 0 until hArr.length()) {
            highlights.add(hArr.getString(i))
        }

        val suggestions = mutableListOf<WellBeingSuggestion>()
        val sArr = JSONArray(suggestionsJson)
        for (i in 0 until sArr.length()) {
            val obj = sArr.getJSONObject(i)
            val id = obj.getString("id")
            suggestions.add(
                WellBeingSuggestion(
                    id = id,
                    title = obj.getString("title"),
                    rationale = obj.getString("rationale"),
                    actionStep = obj.getString("actionStep"),
                    focusTag = obj.getString("focusTag"),
                    impactMinutesPerWeek = obj.optInt("impactMinutesPerWeek", 30),
                    targetAppName = if (obj.has("targetAppName")) obj.getString("targetAppName") else null,
                    isCompleted = id in completedSet,
                ),
            )
        }

        return WeeklyAdvisorReport(
            weekStartIso = weekStartIso,
            generatedAtMillis = generatedAtMillis,
            isSamplePlaceholder = isSamplePlaceholder,
            headline = headline,
            summary = summary,
            wellBeingScore = wellBeingScore,
            patternHighlights = highlights,
            suggestions = suggestions,
        )
    }
}
