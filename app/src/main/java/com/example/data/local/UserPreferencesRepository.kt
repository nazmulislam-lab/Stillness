package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.UserWellBeingSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stillness_settings")

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val WIND_DOWN_HOUR = intPreferencesKey("wind_down_hour")
        val WEEKLY_REVIEW_DAY = intPreferencesKey("weekly_review_day")
    }

    val settings: Flow<UserWellBeingSettings> = context.dataStore.data.map { prefs ->
        UserWellBeingSettings(
            dailyGoalMinutes = prefs[Keys.DAILY_GOAL_MINUTES] ?: 180,
            windDownStartHour = prefs[Keys.WIND_DOWN_HOUR] ?: 22,
            weeklyReviewDayOfWeek = prefs[Keys.WEEKLY_REVIEW_DAY] ?: 7,
        )
    }

    suspend fun updateDailyGoalMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_GOAL_MINUTES] = minutes.coerceIn(30, 600)
        }
    }

    suspend fun updateWindDownStartHour(hour: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WIND_DOWN_HOUR] = hour.coerceIn(19, 23)
        }
    }

    suspend fun updateWeeklyReviewDay(dayOfWeek: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WEEKLY_REVIEW_DAY] = dayOfWeek.coerceIn(1, 7)
        }
    }
}
