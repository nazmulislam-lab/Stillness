package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.UsageRepository
import com.example.data.advisor.PlaceholderWellBeingAdvisorRepository
import com.example.data.local.AppLimitEntity
import com.example.data.local.StillnessDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.usage.DeviceUsageDataSource
import com.example.model.AppCategory
import com.example.model.AppUsageEntry
import com.example.model.DailyUsageSummary
import com.example.model.UserWellBeingSettings
import com.example.model.WeeklyAdvisorReport
import com.example.model.WeeklyUsageSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppSortOption(val label: String) {
    MOST_TIME("Most Time"),
    MOST_LAUNCHES("Most Opens"),
}

enum class AppsTimeframe(val label: String) {
    TODAY("Today"),
    WEEK("7 Days"),
}

data class StillnessUiState(
    val isLoading: Boolean = true,
    val hasUsageAccess: Boolean = false,
    val settings: UserWellBeingSettings = UserWellBeingSettings(),
    val snapshot: WeeklyUsageSnapshot? = null,
    val selectedDayIndex: Int = 6,
    val weeklyReport: WeeklyAdvisorReport? = null,
    val isGeneratingSuggestions: Boolean = false,
    val advisorErrorMessage: String? = null,
    val appLimits: List<AppLimitEntity> = emptyList(),
    val appsSearchQuery: String = "",
    val selectedCategory: AppCategory? = null,
    val appsSortOption: AppSortOption = AppSortOption.MOST_TIME,
    val appsTimeframe: AppsTimeframe = AppsTimeframe.TODAY,
    val editingLimitApp: AppUsageEntry? = null,
) {
    val todaySummary: DailyUsageSummary?
        get() = snapshot?.dailySummaries?.lastOrNull()

    val selectedDaySummary: DailyUsageSummary?
        get() = snapshot?.dailySummaries?.getOrNull(selectedDayIndex) ?: todaySummary
}

@OptIn(ExperimentalCoroutinesApi::class)
class StillnessViewModel(
    private val repository: UsageRepository,
) : ViewModel() {

    private val _snapshot = MutableStateFlow<WeeklyUsageSnapshot?>(null)
    private val _hasUsageAccess = MutableStateFlow(repository.hasUsageAccess())
    private val _isLoading = MutableStateFlow(true)
    private val _selectedDayIndex = MutableStateFlow(6)
    private val _isGeneratingSuggestions = MutableStateFlow(false)
    private val _advisorError = MutableStateFlow<String?>(null)
    private val _appsSearchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    private val _appsSortOption = MutableStateFlow(AppSortOption.MOST_TIME)
    private val _appsTimeframe = MutableStateFlow(AppsTimeframe.TODAY)
    private val _editingLimitApp = MutableStateFlow<AppUsageEntry?>(null)

    private val weeklyReportFlow: StateFlow<WeeklyAdvisorReport?> =
        _snapshot.flatMapLatest { snap ->
            if (snap == null) flowOf(null) else repository.observeWeeklyReport(snap.weekStartIso)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    private val filterStateFlow = combine(
        _appsSearchQuery,
        _selectedCategory,
        _appsSortOption,
        _appsTimeframe,
        _editingLimitApp,
    ) { query, cat, sort, timeframe, editing ->
        FilterBundle(query, cat, sort, timeframe, editing)
    }

    private val advisorStateFlow = combine(
        weeklyReportFlow,
        _isGeneratingSuggestions,
        _advisorError,
    ) { report, generating, error ->
        AdvisorBundle(report, generating, error)
    }

    val uiState: StateFlow<StillnessUiState> = combine(
        _isLoading,
        _hasUsageAccess,
        repository.settingsFlow,
        _snapshot,
        _selectedDayIndex,
        repository.allAppLimitEntitiesFlow,
        filterStateFlow,
        advisorStateFlow,
    ) { values ->
        val loading = values[0] as Boolean
        val access = values[1] as Boolean
        val settings = values[2] as UserWellBeingSettings
        val snapshot = values[3] as WeeklyUsageSnapshot?
        val dayIdx = values[4] as Int
        @Suppress("UNCHECKED_CAST")
        val limits = values[5] as List<AppLimitEntity>
        val filters = values[6] as FilterBundle
        val advisor = values[7] as AdvisorBundle

        StillnessUiState(
            isLoading = loading,
            hasUsageAccess = access,
            settings = settings,
            snapshot = snapshot,
            selectedDayIndex = dayIdx,
            weeklyReport = advisor.report,
            isGeneratingSuggestions = advisor.isGenerating,
            advisorErrorMessage = advisor.error,
            appLimits = limits,
            appsSearchQuery = filters.query,
            selectedCategory = filters.category,
            appsSortOption = filters.sort,
            appsTimeframe = filters.timeframe,
            editingLimitApp = filters.editingApp,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StillnessUiState(),
    )

    init {
        viewModelScope.launch {
            combine(
                repository.settingsFlow,
                repository.appLimitsFlow,
            ) { settings, limits -> settings to limits }
                .distinctUntilChanged()
                .collect { (settings, limits) ->
                    loadUsageData(settings, limits)
                }
        }
    }

    fun refreshUsageStats() {
        viewModelScope.launch {
            val currentSettings = uiState.value.settings
            val limitsMap = uiState.value.appLimits.associate { it.packageName to it.dailyLimitMinutes }
            loadUsageData(currentSettings, limitsMap)
        }
    }

    private suspend fun loadUsageData(
        settings: UserWellBeingSettings,
        limitsMap: Map<String, Int>,
    ) {
        _isLoading.value = true
        val accessGranted = repository.hasUsageAccess()
        _hasUsageAccess.value = accessGranted
        try {
            val snap = repository.loadWeeklySnapshot(
                windDownStartHour = settings.windDownStartHour,
                limitsByPackage = limitsMap,
            )
            _snapshot.value = snap
            if (weeklyReportFlow.value == null) {
                repository.generateAndSaveWeeklyReport(
                    snapshot = snap,
                    dailyGoalMinutes = settings.dailyGoalMinutes,
                    windDownStartHour = settings.windDownStartHour,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _advisorError.value = e.localizedMessage ?: "Unable to load usage statistics."
        } finally {
            _isLoading.value = false
        }
    }

    fun regenerateWeeklySuggestions() {
        val snap = _snapshot.value ?: return
        val settings = uiState.value.settings
        viewModelScope.launch {
            _isGeneratingSuggestions.value = true
            _advisorError.value = null
            try {
                repository.generateAndSaveWeeklyReport(
                    snapshot = snap,
                    dailyGoalMinutes = settings.dailyGoalMinutes,
                    windDownStartHour = settings.windDownStartHour,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _advisorError.value = e.localizedMessage ?: "Could not refresh weekly suggestions."
            } finally {
                _isGeneratingSuggestions.value = false
            }
        }
    }

    fun toggleSuggestionCompleted(suggestionId: String) {
        val report = uiState.value.weeklyReport ?: return
        viewModelScope.launch {
            repository.toggleSuggestionCompleted(report, suggestionId)
        }
    }

    fun selectDayIndex(index: Int) {
        _selectedDayIndex.value = index.coerceIn(0, 6)
    }

    fun updateSearchQuery(query: String) {
        _appsSearchQuery.value = query
    }

    fun selectCategory(category: AppCategory?) {
        _selectedCategory.value = category
    }

    fun updateSortOption(option: AppSortOption) {
        _appsSortOption.value = option
    }

    fun updateTimeframe(timeframe: AppsTimeframe) {
        _appsTimeframe.value = timeframe
    }

    fun openLimitEditor(app: AppUsageEntry?) {
        _editingLimitApp.value = app
    }

    fun saveAppLimit(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch {
            repository.saveAppLimit(packageName, appName, limitMinutes)
            _editingLimitApp.value = null
        }
    }

    fun removeAppLimit(packageName: String) {
        viewModelScope.launch {
            repository.removeAppLimit(packageName)
            _editingLimitApp.value = null
        }
    }

    fun updateDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch {
            repository.updateDailyGoalMinutes(minutes)
        }
    }

    fun updateWindDownHour(hour: Int) {
        viewModelScope.launch {
            repository.updateWindDownStartHour(hour)
        }
    }

    fun updateWeeklyReviewDay(dayOfWeek: Int) {
        viewModelScope.launch {
            repository.updateWeeklyReviewDay(dayOfWeek)
        }
    }

    fun createUsageAccessIntent(): Intent = repository.createUsageAccessIntent()

    fun getAppIcon(packageName: String): Drawable? = repository.getAppIcon(packageName)

    private data class FilterBundle(
        val query: String,
        val category: AppCategory?,
        val sort: AppSortOption,
        val timeframe: AppsTimeframe,
        val editingApp: AppUsageEntry?,
    )

    private data class AdvisorBundle(
        val report: WeeklyAdvisorReport?,
        val isGenerating: Boolean,
        val error: String?,
    )

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val db = StillnessDatabase.getInstance(appContext)
                    val repository = UsageRepository(
                        usageDataSource = DeviceUsageDataSource(appContext),
                        dao = db.stillnessDao(),
                        preferencesRepository = UserPreferencesRepository(appContext),
                        advisorRepository = PlaceholderWellBeingAdvisorRepository(),
                    )
                    return StillnessViewModel(repository) as T
                }
            }
    }
}
