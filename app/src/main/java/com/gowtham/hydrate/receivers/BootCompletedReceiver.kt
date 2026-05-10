package com.gowtham.hydrate.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gowtham.hydrate.di.HydrateEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + Job()).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(context, HydrateEntryPoint::class.java)
                val repository = entryPoint.repository()
                val scheduler = entryPoint.scheduler()
                val preferences = repository.getPreferencesSnapshot()
                val schedule = entryPoint.generateScheduleUseCase().invoke(preferences, emptyList())
                scheduler.scheduleDailyReminders(preferences, schedule)
                scheduler.scheduleMidnightReschedule()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
