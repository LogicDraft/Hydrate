package com.gowtham.hydrate.domain.usecase

import com.gowtham.hydrate.data.local.WaterLogEntity
import com.gowtham.hydrate.data.model.ReminderSlot
import com.gowtham.hydrate.data.model.UserPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.math.max
import java.time.temporal.ChronoUnit

class GenerateScheduleUseCase {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    operator fun invoke(
        preferences: UserPreferences,
        logs: List<WaterLogEntity>,
        now: Instant = Instant.now(),
    ): List<ReminderSlot> {
        val zoneId = ZoneId.systemDefault()
        val nowDateTime = now.atZone(zoneId).toLocalDateTime()
        val currentDate = now.atZone(zoneId).toLocalDate()
        val wakeDateTime = currentDate.atTime(preferences.wakeTime)
        val sleepDateTime = if (preferences.sleepTime.isAfter(preferences.wakeTime)) {
            currentDate.atTime(preferences.sleepTime)
        } else {
            currentDate.plusDays(1).atTime(preferences.sleepTime)
        }

        val activeMinutes = max(1, ChronoUnit.MINUTES.between(wakeDateTime, sleepDateTime).toInt())
        val cupSize = preferences.cupSizeMl.coerceAtLeast(1)
        val cupsRequired = max(1, ceil(preferences.dailyGoalMl / cupSize.toDouble()).toInt())
        val intervalMinutes = activeMinutes.toDouble() / cupsRequired.toDouble()
        val timestamps = (0 until cupsRequired).map { index ->
            wakeDateTime.plusMinutes((index * intervalMinutes).roundToLong())
        }.distinct().sorted()

        val logTimes = logs.map { Instant.ofEpochMilli(it.timestampMillis).atZone(zoneId).toLocalDateTime() }

        return timestamps.mapIndexed { index, slotTime ->
            val previousSlot = timestamps.getOrNull(index - 1) ?: wakeDateTime.minusMinutes(20)
            val nextSlot = timestamps.getOrNull(index + 1) ?: sleepDateTime.plusMinutes(20)
            val amount = if (index < timestamps.lastIndex) cupSize else preferences.dailyGoalMl - (cupSize * timestamps.lastIndex)
            val completed = logTimes.any { logged ->
                (logged.isAfter(previousSlot) || logged.isEqual(previousSlot)) &&
                    (logged.isBefore(nextSlot) || logged.isEqual(nextSlot))
            }
            val current = (nowDateTime.isAfter(slotTime) || nowDateTime.isEqual(slotTime)) && nowDateTime.isBefore(nextSlot)
            val upcoming = nowDateTime.isBefore(slotTime)
            ReminderSlot(
                timestampMillis = slotTime.atZone(zoneId).toInstant().toEpochMilli(),
                timeLabel = slotTime.format(timeFormatter),
                amountMl = amount.coerceAtLeast(1),
                cumulativeMl = ((index + 1) * cupSize).coerceAtMost(preferences.dailyGoalMl),
                completed = completed,
                current = current,
                upcoming = upcoming,
            )
        }
    }
}
