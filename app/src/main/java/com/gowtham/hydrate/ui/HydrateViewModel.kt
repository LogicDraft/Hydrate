package com.gowtham.hydrate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gowtham.hydrate.data.model.HistorySummary
import com.gowtham.hydrate.data.model.ReminderSlot
import com.gowtham.hydrate.data.model.TodaySummary
import com.gowtham.hydrate.data.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HydrateUiState(
    val preferences: UserPreferences = UserPreferences(),
    val todaySummary: TodaySummary = TodaySummary(0, 2500, 0, "Great start.", "--:--", 0),
    val schedule: List<ReminderSlot> = emptyList(),
    val historySummary: HistorySummary = HistorySummary(0, 0, 0, 0, 0),
    val todayLogs: List<Pair<Long, Int>> = emptyList(),
    val needsOnboarding: Boolean = true,
)

class HydrateViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HydrateUiState())
    val uiState: StateFlow<HydrateUiState> = _uiState.asStateFlow()

    fun initialize() {
        _uiState.value = _uiState.value.copy(needsOnboarding = true)
    }

    fun saveOnboarding() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(needsOnboarding = false)
        }
    }
}
