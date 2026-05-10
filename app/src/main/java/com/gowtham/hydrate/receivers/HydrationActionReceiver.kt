package com.gowtham.hydrate.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gowtham.hydrate.di.HydrateEntryPoint
import com.gowtham.hydrate.domain.scheduler.HydrationNotificationManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.Instant

class HydrationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + Job()).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(context, HydrateEntryPoint::class.java)
                val repository = entryPoint.repository()
                val scheduler = entryPoint.scheduler()
                val notificationManager = entryPoint.notificationManager()
                val preferences = repository.getPreferencesSnapshot()
                val amount = intent.getIntExtra(HydrationNotificationManager.EXTRA_AMOUNT_ML, preferences.cupSizeMl)
                val requestCode = intent.getIntExtra(HydrationNotificationManager.EXTRA_REQUEST_CODE, 0)
                when (intent.action) {
                    HydrationNotificationManager.ACTION_I_DRANK -> {
                        repository.logWater(amount, Instant.now())
                        notificationManager.cancel(requestCode)
                        scheduler.cancelAllReminders()
                        val updatedPreferences = repository.getPreferencesSnapshot()
                        val schedule = entryPoint.generateScheduleUseCase().invoke(
                            updatedPreferences,
                            repository.todayLogs.first(),
                            repository.skippedReminderTimestamps.first(),
                        )
                        scheduler.scheduleDailyReminders(updatedPreferences, schedule)
                        scheduler.scheduleMidnightReschedule()
                    }
                    HydrationNotificationManager.ACTION_SNOOZE -> {
                        val snoozeMillis = 15 * 60_000L
                        notificationManager.cancel(requestCode)
                        scheduler.scheduleSnoozedReminder(
                            amountMl = amount,
                            triggerAtMillis = System.currentTimeMillis() + snoozeMillis,
                        )
                    }
                    HydrationNotificationManager.ACTION_SKIP -> {
                        notificationManager.cancel(requestCode)
                        val slotTimestamp = intent.getLongExtra(HydrationAlarmReceiver.EXTRA_TIMESTAMP_MILLIS, 0L)
                        if (slotTimestamp != 0L) {
                            repository.skipReminderSlot(slotTimestamp)
                            scheduler.cancelSlotReminder(slotTimestamp)
                            val updatedPreferences = repository.getPreferencesSnapshot()
                            val schedule = entryPoint.generateScheduleUseCase().invoke(
                                updatedPreferences,
                                repository.todayLogs.first(),
                                repository.skippedReminderTimestamps.first(),
                            )
                            scheduler.scheduleDailyReminders(updatedPreferences, schedule)
                            scheduler.scheduleMidnightReschedule()
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
