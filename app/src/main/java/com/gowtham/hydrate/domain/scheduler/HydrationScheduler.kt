package com.gowtham.hydrate.domain.scheduler

import com.gowtham.hydrate.data.model.ReminderSlot
import com.gowtham.hydrate.data.model.UserPreferences

interface HydrationScheduler {
    fun scheduleDailyReminders(preferences: UserPreferences, schedule: List<ReminderSlot>)
    fun scheduleSnoozedReminder(amountMl: Int, triggerAtMillis: Long)
    fun scheduleMidnightReschedule()
    fun cancelAllReminders()
}
