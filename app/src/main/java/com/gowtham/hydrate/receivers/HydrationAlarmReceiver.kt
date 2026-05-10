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
                val latest = repository.getLatestLogTimestamp()
                val shouldSkip = latest != null && Duration.between(latest, Instant.now()).toMinutes() < 20
                if (!shouldSkip) {
                    notificationManager.showReminder(amount, requestCode)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_AMOUNT_ML = HydrationNotificationManager.EXTRA_AMOUNT_ML
        const val EXTRA_TIMESTAMP_MILLIS = "extra_timestamp_millis"
        const val EXTRA_REQUEST_CODE = HydrationNotificationManager.EXTRA_REQUEST_CODE
    }
}
