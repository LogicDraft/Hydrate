package com.gowtham.hydrate.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gowtham.hydrate.di.HydrateEntryPoint
import com.gowtham.hydrate.domain.scheduler.HydrationNotificationManager
import com.gowtham.hydrate.data.model.ReminderAlertMode
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

class HydrationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + Job())
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(context, HydrateEntryPoint::class.java)
                val repository = entryPoint.repository()
                val notificationManager = entryPoint.notificationManager()
                val amount = intent.getIntExtra(EXTRA_AMOUNT_ML, 250)
                val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)
                val slotTimestampMillis = intent.getLongExtra(EXTRA_TIMESTAMP_MILLIS, 0L)
                val preferences = repository.getPreferencesSnapshot()
                val latest = repository.getLatestLogTimestamp()
                val now = Instant.now()
                val minutesSinceLastLog = latest?.let { Duration.between(it, now).toMinutes() }
                val shouldSkip = minutesSinceLastLog != null && minutesSinceLastLog in 0..19
                if (!shouldSkip) {
                    notificationManager.showReminder(
                        amountMl = amount,
                        requestCode = requestCode,
                        slotTimestampMillis = slotTimestampMillis,
                        vibrationOnly = preferences.reminderAlertMode == ReminderAlertMode.VIBRATION_ONLY,
                        phoneRingtone = preferences.reminderAlertMode == ReminderAlertMode.PHONE_RINGTONE,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_AMOUNT_ML = HydrationNotificationManager.EXTRA_AMOUNT_ML
        const val EXTRA_TIMESTAMP_MILLIS = HydrationNotificationManager.EXTRA_TIMESTAMP_MILLIS
        const val EXTRA_REQUEST_CODE = HydrationNotificationManager.EXTRA_REQUEST_CODE
    }
}
