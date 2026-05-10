package com.gowtham.hydrate.domain.usecase

import com.gowtham.hydrate.data.local.WaterLogEntity
import com.gowtham.hydrate.data.model.HistorySummary
import com.gowtham.hydrate.data.model.ReminderSlot
import com.gowtham.hydrate.data.model.TodaySummary
import com.gowtham.hydrate.data.model.UserPreferences
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max

class CalculateTodaySummaryUseCase {

    operator fun invoke(
        preferences: UserPreferences,
        logs: List<WaterLogEntity>,
        schedule: List<ReminderSlot>,
        historySummary: HistorySummary,
        weatherSuggestion: String?,
        now: Instant = Instant.now(),
    ): TodaySummary {
        val totalMl = logs.sumOf { it.amountMl }
        val percent = if (preferences.dailyGoalMl <= 0) 0 else ((totalMl.toDouble() / preferences.dailyGoalMl) * 100).toInt().coerceAtLeast(0)
        val message = when (percent) {
            in 0..25 -> "Great start."
            in 26..50 -> "Keep going."
            in 51..75 -> "You’re doing great."
            in 76..99 -> "Almost there."
            else -> "Goal complete."
        }
        val nextSlot = schedule.firstOrNull { it.upcoming } ?: schedule.firstOrNull { it.current }
        val countdown = nextSlot?.let {
            val diff = Duration.between(now, Instant.ofEpochMilli(it.timestampMillis))
            if (diff.isNegative || diff.isZero) {
                "Now"
            } else {
                val totalMinutes = diff.toMinutes()
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                buildString {
                    if (hours > 0) append("${hours}h ")
                    append("${minutes}m")
                }.trim()
            }
        } ?: "--"

        val targetByNow = schedule
            .filter { Instant.ofEpochMilli(it.timestampMillis) <= now }
            .sumOf { it.amountMl }
        val behindMl = (targetByNow - totalMl).coerceAtLeast(0)
        val cupSize = max(1, preferences.cupSizeMl)
        val catchUpCups = ceil(behindMl / cupSize.toDouble()).toInt().coerceAtLeast(1)
        val carryOverSuggestion = if (behindMl >= (cupSize / 2) && percent < 100) {
            "You're $behindMl ml behind, drink $catchUpCups cups soon."
        } else {
            null
        }

        return TodaySummary(
            totalMl = totalMl,
            goalMl = preferences.dailyGoalMl,
            percent = percent,
            message = message,
            nextReminderLabel = nextSlot?.timeLabel ?: "--:--",
            nextReminderCountdown = countdown,
            streakDays = historySummary.currentStreak,
            carryOverSuggestion = carryOverSuggestion,
            weatherSuggestion = weatherSuggestion,
        )
    }
}
