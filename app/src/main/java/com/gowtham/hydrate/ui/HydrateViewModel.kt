package com.gowtham.hydrate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowtham.hydrate.data.local.DailyStatsEntity
import com.gowtham.hydrate.data.local.WaterLogEntity
import com.gowtham.hydrate.data.model.HistorySummary
import com.gowtham.hydrate.data.model.ReminderSlot
import com.gowtham.hydrate.data.model.TodaySummary
import com.gowtham.hydrate.data.model.UserPreferences
import com.gowtham.hydrate.domain.scheduler.HydrationNotificationManager
import com.gowtham.hydrate.data.repository.HydrationRepository
import com.gowtham.hydrate.domain.scheduler.HydrationScheduler
import com.gowtham.hydrate.domain.usecase.CalculateHistorySummaryUseCase
import com.gowtham.hydrate.domain.usecase.CalculateTodaySummaryUseCase
import com.gowtham.hydrate.domain.usecase.GenerateScheduleUseCase
import com.gowtham.hydrate.domain.usecase.GetWeatherAwareSuggestionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HydrateUiState(
    val preferences: UserPreferences = UserPreferences(),
    val todaySummary: TodaySummary = TodaySummary(0, 2500, 0, "Great start.", "--:--", "--", 0, null, null),
    val schedule: List<ReminderSlot> = emptyList(),
    val historySummary: HistorySummary = HistorySummary(0, 0, 0, 0, 0, 0),
    val todayLogs: List<WaterLogEntity> = emptyList(),
    val recentStats: List<DailyStatsEntity> = emptyList(),
    val needsOnboarding: Boolean = true,
    val showTabTips: Boolean = false,
    val shouldCelebrateGoal: Boolean = false,
)

@HiltViewModel
class HydrateViewModel @Inject constructor(
    private val repository: HydrationRepository,
    private val generateScheduleUseCase: GenerateScheduleUseCase,
    private val calculateTodaySummaryUseCase: CalculateTodaySummaryUseCase,
    private val calculateHistorySummaryUseCase: CalculateHistorySummaryUseCase,
    private val getWeatherAwareSuggestionUseCase: GetWeatherAwareSuggestionUseCase,
    private val scheduler: HydrationScheduler,
    private val notificationManager: HydrationNotificationManager,
) : ViewModel() {

    private val weatherSuggestion = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val uiState: StateFlow<HydrateUiState> = combine(
        repository.preferences,
        repository.skippedReminderTimestamps,
        repository.todayLogs,
        repository.recentStats,
        repository.tabTipsSeen,
        repository.dailyGoalCelebrationDate,
        weatherSuggestion,
    ) { preferences, skipped, logs, stats, tipsSeen, celebrationDate, weatherText ->
        val schedule = generateScheduleUseCase(preferences, logs, skipped, Instant.now())
        val historySummary = calculateHistorySummaryUseCase(stats)
        val todaySummary = calculateTodaySummaryUseCase(
            preferences,
            logs,
            schedule,
            historySummary,
            weatherText,
            Instant.now(),
        )
        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        val shouldCelebrateGoal = todaySummary.percent >= 100 && celebrationDate != today
        HydrateUiState(
            preferences = preferences,
            todaySummary = todaySummary,
            schedule = schedule,
            historySummary = historySummary,
            todayLogs = logs,
            recentStats = stats,
            needsOnboarding = !preferences.onboarded,
            showTabTips = preferences.onboarded && !tipsSeen,
            shouldCelebrateGoal = shouldCelebrateGoal,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HydrateUiState())

    init {
        refreshWeatherSuggestion()
        observeLockScreenSummary()
    }

    fun savePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            repository.savePreferences(preferences.copy(onboarded = true))
            repository.updateOnboardingComplete()
            refreshWeatherSuggestion()
            syncReminders()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun quickAdd(amountMl: Int) {
        viewModelScope.launch {
            val now = Instant.now()
            val lastLog = repository.getLatestLogTimestamp()
            
            // Layer 1: Rate Limiting
            if (lastLog != null) {
                val minsSince = java.time.Duration.between(lastLog, now).toMinutes()
                if (minsSince < 20) {
                    _errorMessage.value = "You just logged. Wait 20 min before logging again."
                    return@launch
                }
            }

            // Layer 4: Daily Cap
            val todayTotal = uiState.value.todaySummary.totalMl
            val goal = uiState.value.todaySummary.goalMl
            val maxCap = (goal * 1.2).toInt()
            if (todayTotal + amountMl > maxCap) {
                _errorMessage.value = "Daily cap reached. You cannot exceed 120% of your daily goal."
                return@launch
            }

            // Layer 2: Schedule-Locked Logging
            val schedule = uiState.value.schedule
            if (schedule.isNotEmpty()) {
                val closestSlot = schedule.minByOrNull { Math.abs(it.timestampMillis - now.toEpochMilli()) }
                if (closestSlot != null) {
                    val minsDiff = Math.abs(closestSlot.timestampMillis - now.toEpochMilli()) / 60_000
                    if (minsDiff > 15) {
                        val nextSlot = schedule.firstOrNull { it.timestampMillis > now.toEpochMilli() }
                        val timeLabel = nextSlot?.timeLabel ?: "tomorrow"
                        _errorMessage.value = "Outside logging window. Next slot at $timeLabel."
                        return@launch
                    }
                }
            }

            repository.logWater(amountMl)
            syncReminders()
        }
    }

    fun undoLastLog() {
        viewModelScope.launch {
            repository.undoLastLog()
            syncReminders()
        }
    }

    fun snoozeSlot(slotTimestampMillis: Long, amountMl: Int) {
        viewModelScope.launch {
            repository.skipReminderSlot(slotTimestampMillis)
            scheduler.cancelSlotReminder(slotTimestampMillis)
            scheduler.scheduleSnoozedReminder(amountMl = amountMl, triggerAtMillis = System.currentTimeMillis() + 15 * 60_000L)
            syncReminders()
        }
    }

    fun skipSlot(slotTimestampMillis: Long) {
        viewModelScope.launch {
            repository.skipReminderSlot(slotTimestampMillis)
            scheduler.cancelSlotReminder(slotTimestampMillis)
            syncReminders()
        }
    }

    fun resetToday() {
        viewModelScope.launch {
            repository.clearToday()
            syncReminders()
        }
    }

    fun eraseAllData() {
        viewModelScope.launch {
            repository.eraseAllData()
            scheduler.cancelAllReminders()
        }
    }

    fun dismissTabTips() {
        viewModelScope.launch {
            repository.markTabTipsSeen()
        }
    }

    fun acknowledgeGoalCelebrationShown() {
        viewModelScope.launch {
            val today = LocalDate.now(ZoneId.systemDefault()).toString()
            repository.markGoalCelebratedForDate(today)
        }
    }

    private suspend fun syncReminders() {
        val preferences = repository.getPreferencesSnapshot()
        val logs = uiState.value.todayLogs
        val skipped = repository.skippedReminderTimestamps.first()
        val schedule = generateScheduleUseCase(preferences, logs, skipped, Instant.now())
        scheduler.cancelAllReminders()
        scheduler.scheduleDailyReminders(preferences, schedule)
        scheduler.scheduleMidnightReschedule()
    }

    private fun refreshWeatherSuggestion() {
        viewModelScope.launch {
            weatherSuggestion.value = getWeatherAwareSuggestionUseCase()
        }
    }

    private fun observeLockScreenSummary() {
        viewModelScope.launch {
            uiState.collect { state ->
                notificationManager.showLockScreenSummary(
                    percent = state.todaySummary.percent,
                    totalMl = state.todaySummary.totalMl,
                    goalMl = state.todaySummary.goalMl,
                )
            }
        }
    }
}
