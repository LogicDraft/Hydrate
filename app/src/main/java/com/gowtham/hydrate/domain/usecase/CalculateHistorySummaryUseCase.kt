package com.gowtham.hydrate.domain.usecase

import com.gowtham.hydrate.data.local.DailyStatsEntity
import com.gowtham.hydrate.data.model.HistorySummary
import java.time.LocalDate

class CalculateHistorySummaryUseCase {

    operator fun invoke(stats: List<DailyStatsEntity>): HistorySummary {
        if (stats.isEmpty()) {
            return HistorySummary(0, 0, 0, 0, 0)
        }

        val byDate = stats.associateBy { LocalDate.parse(it.date) }
        val latestDate = byDate.keys.maxOrNull() ?: return HistorySummary(0, 0, 0, 0, 0)

        var currentDate = latestDate
        while (!byDate.containsKey(currentDate) && currentDate.isAfter(latestDate.minusDays(7))) {
            currentDate = currentDate.minusDays(1)
        }

        var currentStreak = 0
        var cursor = currentDate
        while (true) {
            val day = byDate[cursor] ?: break
            if (!day.goalCompleted) break
            currentStreak++
            cursor = cursor.minusDays(1)
        }

        var longestStreak = 0
        var run = 0
        val sortedDates = byDate.keys.sorted()
        var previousDate: LocalDate? = null
        for (date in sortedDates) {
            val item = byDate[date] ?: continue
            if (item.goalCompleted && (previousDate == null || previousDate.plusDays(1) == date)) {
                run++
            } else if (item.goalCompleted) {
                run = 1
            } else {
                run = 0
            }
            longestStreak = maxOf(longestStreak, run)
            previousDate = date
        }

        val bestDay = stats.maxByOrNull { it.totalMl }?.totalMl ?: 0
        val average = stats.map { it.totalMl }.average().toInt()

        val weeklyDates = (0 until 7).map { latestDate.minusDays(it.toLong()) }
        val weeklyCompletion = weeklyDates.count { byDate[it]?.goalCompleted == true }
        val weeklyPercent = ((weeklyCompletion / 7.0) * 100).toInt()

        return HistorySummary(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            bestDayMl = bestDay,
            averageMl = average,
            weeklyCompletionPercent = weeklyPercent,
        )
    }
}
