package com.gowtham.hydrate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowtham.hydrate.data.local.DailyStatsEntity
import com.gowtham.hydrate.data.local.WaterLogEntity
import com.gowtham.hydrate.data.model.HistorySummary
import com.gowtham.hydrate.data.model.ReminderSlot
import com.gowtham.hydrate.data.model.TodaySummary
import com.gowtham.hydrate.data.model.UserPreferences
import com.gowtham.hydrate.data.repository.HydrationRepository
import com.gowtham.hydrate.domain.scheduler.HydrationScheduler
import com.gowtham.hydrate.domain.usecase.CalculateHistorySummaryUseCase
import com.gowtham.hydrate.domain.usecase.CalculateTodaySummaryUseCase
import com.gowtham.hydrate.domain.usecase.GenerateScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HydrateUiState(
    val preferences: UserPreferences = UserPreferences(),
    val todaySummary: TodaySummary = TodaySummary(0, 2500, 0, "Great start.", "--:--", "--", 0),
    val schedule: List<ReminderSlot> = emptyList(),
    val historySummary: HistorySummary = HistorySummary(0, 0, 0, 0, 0),
    val todayLogs: List<WaterLogEntity> = emptyList(),
    val recentStats: List<DailyStatsEntity> = emptyList(),
    val needsOnboarding: Boolean = true,
)

@HiltViewModel
class HydrateViewModel @Inject constructor(
    private val repository: HydrationRepository,
    private val generateScheduleUseCase: GenerateScheduleUseCase,
    private val calculateTodaySummaryUseCase: CalculateTodaySummaryUseCase,
    private val calculateHistorySummaryUseCase: CalculateHistorySummaryUseCase,
    private val scheduler: HydrationScheduler,
) : ViewModel() {

    val uiState: StateFlow<HydrateUiState> = combine(
        repository.preferences,
        repository.todayLogs,
        repository.recentStats,
    ) { preferences, logs, stats ->
        val schedule = generateScheduleUseCase(preferences, logs, Instant.now())
        val historySummary = calculateHistorySummaryUseCase(stats)
        val todaySummary = calculateTodaySummaryUseCase(preferences, logs, schedule, historySummary, Instant.now())
        HydrateUiState(
            preferences = preferences,
            todaySummary = todaySummary,
            schedule = schedule,
            historySummary = historySummary,
            todayLogs = logs,
            recentStats = stats,
            needsOnboarding = !preferences.onboarded,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HydrateUiState())

    fun savePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            repository.savePreferences(preferences.copy(onboarded = true))
            repository.updateOnboardingComplete()
            syncReminders()
        }
    }

    fun quickAdd(amountMl: Int) {
        viewModelScope.launch {
            repository.logWater(amountMl)
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

    private suspend fun syncReminders() {
        val preferences = repository.getPreferencesSnapshot()
        val logs = repository.todayLogs.map { it }.stateIn(viewModelScope).value
        val schedule = generateScheduleUseCase(preferences, logs, Instant.now())
        scheduler.cancelAllReminders()
        scheduler.scheduleDailyReminders(preferences, schedule)
        scheduler.scheduleMidnightReschedule()
    }
}
