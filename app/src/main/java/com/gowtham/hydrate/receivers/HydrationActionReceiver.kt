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
import java.time.Instant

class HydrationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + Job()).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(context, HydrateEntryPoint::class.java)
                val repository = entryPoint.repository()
                val scheduler = entryPoint.scheduler()
                val preferences = repository.getPreferencesSnapshot()
                val amount = intent.getIntExtra(HydrationNotificationManager.EXTRA_AMOUNT_ML, preferences.cupSizeMl)
                when (intent.action) {
                    HydrationNotificationManager.ACTION_I_DRANK -> {
                        repository.logWater(amount, Instant.now())
                        scheduler.cancelAllReminders()
                        val updatedPreferences = repository.getPreferencesSnapshot()
                        val schedule = entryPoint.generateScheduleUseCase().invoke(updatedPreferences, emptyList())
                        scheduler.scheduleDailyReminders(updatedPreferences, schedule)
                        scheduler.scheduleMidnightReschedule()
                    }
                    HydrationNotificationManager.ACTION_SNOOZE -> {
                        val snoozeMillis = preferences.snoozeMinutes * 60_000L
                        scheduler.scheduleSnoozedReminder(
                            amountMl = amount,
                            triggerAtMillis = System.currentTimeMillis() + snoozeMillis,
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
