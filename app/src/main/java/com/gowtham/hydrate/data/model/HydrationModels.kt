package com.gowtham.hydrate.data.model

import java.time.LocalTime

enum class CupSize(val milliliters: Int, val label: String) {
    CUP_150(150, "150 ml"),
    CUP_200(200, "200 ml"),
    CUP_250(250, "250 ml"),
    CUSTOM(0, "Custom")
}

data class UserPreferences(
    val wakeTime: LocalTime = LocalTime.of(7, 0),
    val sleepTime: LocalTime = LocalTime.of(23, 0),
    val dailyGoalMl: Int = 2500,
    val cupSizeMl: Int = 250,
    val notificationsEnabled: Boolean = true,
    val snoozeMinutes: Int = 60,
    val onboarded: Boolean = false,
)

data class ReminderSlot(
    val timestampMillis: Long,
    val timeLabel: String,
    val amountMl: Int,
    val cumulativeMl: Int,
    val completed: Boolean,
    val current: Boolean,
    val upcoming: Boolean,
    val skipped: Boolean = false,
)

data class TodaySummary(
    val totalMl: Int,
    val goalMl: Int,
    val percent: Int,
    val message: String,
    val nextReminderLabel: String,
    val nextReminderCountdown: String,
    val streakDays: Int,
    val carryOverSuggestion: String? = null,
    val weatherSuggestion: String? = null,
)

data class HistorySummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val bestDayMl: Int,
    val averageMl: Int,
    val weeklyGoalHitDays: Int,
    val weeklyCompletionPercent: Int,
)
